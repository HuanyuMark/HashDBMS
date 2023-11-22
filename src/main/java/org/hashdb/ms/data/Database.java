package org.hashdb.ms.data;

import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.config.DBFileConfig;
import org.hashdb.ms.exception.ServiceStoppedException;
import org.hashdb.ms.exception.WorkerInterruptedException;
import org.hashdb.ms.persistent.PersistentService;
import org.hashdb.ms.util.AsyncService;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Date: 2023/11/21 1:45
 * java.util.PriorityQueue 优先队列
 * ThreeSet 有序集合， 可比较
 * BitSet 位集 => BitMap
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class Database implements Iterable<HValue> {
    private final DatabaseInfos info;
    /**
     * 可以用 {@link String} 来当作键名来查询， 原因参见 {@link #get(String key)}
     * 因为一切操作都需要通过opsEventQueue 来进行，且只有一个消费者线程，所以天生线程安全，故而使用
     * {@link HashMap}
     * */
    protected final Map<HKey, HValue> table = new HashMap<>();
    protected final AtomicReference<ScheduledFuture<?>> saveTask = new AtomicReference<>();
    protected final AtomicReference<CompletableFuture<?>> taskDequeueConsumer = new AtomicReference<>();
    protected final AtomicBoolean receiveNewTask = new AtomicBoolean(false);
    protected final BlockingDeque<DequeTask<?>> taskDeque = new LinkedBlockingDeque<>();
    protected void startDatabaseDaemonTask(){
        startSaveTask();
        startTaskDequeConsumer();
    }
    public CompletableFuture<Boolean> startTaskDequeConsumer(){
        // 正在接收新任务，则直接 返回，启动不成功
        if(receiveNewTask.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(false);
        }
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Supplier<CompletableFuture<?>> taskDequeConsumerSupplier = ()-> AsyncService.submit(()->{
            future.complete(true);
            while (true) {
                if(taskDeque.size() == 0 && receiveNewTask.compareAndSet(false,false)) {
                    taskDequeueConsumer.set(null);
                    break;
                }
                DequeTask<?> task;
                try {
                    task = taskDeque.take();
                } catch (InterruptedException e) {
                    throw new WorkerInterruptedException(e);
                }
                task.get();
            }
        });

        // 需要接收新任务
        // 消费线程在running, 就不开启新消费任务, 返回， 启动成功
        // 不在running, 就开启一个新消费任务
        taskDequeueConsumer.compareAndSet(null, taskDequeConsumerSupplier.get());
        return future;
    }
    public CompletableFuture<Boolean> stopTaskDequeConsumer(){
        receiveNewTask.compareAndSet(true, false);
        CompletableFuture<?> consumerTask = taskDequeueConsumer.get();
        if(consumerTask == null) {
            return CompletableFuture.completedFuture(true);
        }
        return consumerTask.thenApply(v->true);
    }

    public boolean startSaveTask(){
        Supplier<ScheduledFuture<?>> saveTaskSupplier = ()->{
            DBFileConfig dbFileConfig = HashDBMSApp.ctx().getBean(DBFileConfig.class);
            PersistentService persistentService = HashDBMSApp.ctx().getBean(PersistentService.class);
            final long nextSaveTime = dbFileConfig.getSaveInterval() + info.getLastSaveTime().getTime();
            long initDelay = nextSaveTime - System.currentTimeMillis();
            if(initDelay < 0) {
                initDelay += dbFileConfig.getSaveInterval();
            }
            return AsyncService.setInterval(() -> {
                persistentService.persist(this);
            }, dbFileConfig.getSaveInterval(), initDelay);
        };

        saveTask.compareAndSet(null, saveTaskSupplier.get());
        return true;
    }

    public boolean stopSaveTask(){
        if (saveTask.compareAndSet(null, null)) {
            return true;
        }
        // 如果在保存任务中，执行线程被阻塞在IO,网络操作中，则不直接
        // 抛出中断异常， 让执行线程继续执行
        saveTask.get().cancel(false);
        saveTask.set(null);
        return true;
    }
    public Database(String name, Date createTime) {
        this(new DatabaseInfos(name,createTime));
    }
    public Database(DatabaseInfos databaseInfos) {
        this.info = databaseInfos;
        startDatabaseDaemonTask();
    }
    public Database(String name, Date createTime, @NotNull Map<HKey,Object> initialValues) {
        this(new DatabaseInfos(name,createTime), initialValues);
    }
    public Database(DatabaseInfos databaseInfos, @NotNull Map<HKey,Object> initialValues) {
        this.info = databaseInfos;
        initialValues.forEach((k,v)-> table.put(k, new HValue(k,v)));
        startDatabaseDaemonTask();
    }
    public DatabaseInfos getInfos(){
        return info;
    }
    public HValue set(HKey HKey, Object value) {
        checkTaskQueueConsumer();
        DequeTask<HValue> task = DequeTask.of(() -> {
            HKey.clearWhenExpiredBy(this);
            return table.put(HKey, new HValue(HKey,value));
        });
        taskDeque.add(task);
        return task.result();
    }
    public HValue set(String key, Date expiredDate, Object value) {
        return set(new HKey(key, expiredDate),value);
    }
    public HValue get(HKey key) {
        return get(key.getName());
    }
    /**
     * @param key 键名,你可以看 {@link HKey} 类的 {@link HKey#equals(Object)} 与
     *            {@link HKey#hashCode()} 方法的实现，{@link HKey} 与 {@link String}
     *            在 {@link Map} 容器中, 是等效的
     */
    public HValue get(String key) {
        checkTaskQueueConsumer();
        DequeTask<HValue> task = DequeTask.of(() ->table.get(key));
        taskDeque.add(task);
        return task.result();
    }
    public List<HKey> keys(int count){
        checkTaskQueueConsumer();
        DequeTask<List<HKey>> task = DequeTask.of(() ->table.keySet().stream().limit(count).toList());
        taskDeque.add(task);
        return task.result();
    }
    public List<HKey> keys(){
        return keys(table.size());
    }
    public List<HValue> values(int count){
        checkTaskQueueConsumer();
        DequeTask<List<HValue>> task = DequeTask.of(() -> table.values().stream().limit(count).toList());
        taskDeque.add(task);
        return task.result();
    }
    public List<HValue> values(){
        return values(table.size());
    }
    public HValue del(@NotNull HKey key) {
        return del(key.getName());
    }
    public HValue del(String key) {
        checkTaskQueueConsumer();
        DequeTask<HValue> task = DequeTask.of(() -> {
            HValue value = table.remove(key);
            if (value != null) {
                value.key().cancelClearExpired();
            }
            return value;
        });
        taskDeque.add(task);
        return task.result();
    }
    public HValue delHighPriority(@NotNull HKey key) {
        return delHighPriority(key.getName());
    }
    public HValue delHighPriority(String key) {
        checkTaskQueueConsumer();
        DequeTask<HValue> task = DequeTask.of(() -> {
            HValue value = table.remove(key);
            if (value!= null) {
                value.key().cancelClearExpired();
            }
            return value;
        });
        taskDeque.addFirst(task);
        return task.result();
    }
    public void clear(){
        checkTaskQueueConsumer();
        DequeTask<Void> task = DequeTask.of(() -> {
            table.values().forEach(v-> v.key().cancelClearExpired());
            table.clear();
        });
        taskDeque.add(task);
    }
    public List<HValue> delByKeyword(String keyword) {
        checkTaskQueueConsumer();
        DequeTask<List<HValue>> task = DequeTask.of(() ->
                table.values().parallelStream()
                .filter(value -> value.key().getName().matches(keyword))
                .peek(v-> del(keyword))
                .toList()
        );
        return task.result();
    }
    public HValue rpl(String key, Date expiredDate, Object value) {
        return rpl(new HKey(key, expiredDate), value);
    }
    public HValue rpl(HKey HKey, Object value) {
        checkTaskQueueConsumer();
        DequeTask<HValue> task = DequeTask.of(() -> {
            HKey.clearWhenExpiredBy(this);
            return table.replace(HKey, new HValue(HKey,value));
        });
        taskDeque.add(task);
        return task.result();
    }
    public boolean containsKey(HKey key) {
        return containsKey(key.getName());
    }
    public boolean containsKey(String key) {
        checkTaskQueueConsumer();
        DequeTask<Boolean> task = DequeTask.of(() -> table.containsKey(key));
        taskDeque.add(task);
        return task.result();
    }
    public void delay(String key, long millis) {
        checkTaskQueueConsumer();
        DequeTask<Void> task = DequeTask.of(() -> {
            HValue value = table.get(key);
            if (value == null) {
                return;
            }
            HKey HKey = value.key().withDelay(millis);
            HKey.clearWhenExpiredBy(this);
            table.put(HKey, value);
        });
        taskDeque.add(task);
    }
    public void delay(HKey key, long millis) {
        delay(key.getName(),millis);
    }
    public void delayByKeyword(String keyword, long millis) {
        checkTaskQueueConsumer();
        DequeTask<Void> task = DequeTask.of(() -> {
            table.entrySet().parallelStream()
                    .filter(entry-> entry.getKey().getName().matches(keyword))
                    .forEach(entry-> delay(entry.getKey(), millis));
        });
        taskDeque.add(task);
    }
    public long count(){
        return table.size();
    }
    public long countByKeyword(String keyword) {
        checkTaskQueueConsumer();
        DequeTask<Long> task = DequeTask.of(() -> table.values().parallelStream()
                .filter(v -> v.key().getName().matches(keyword))
                .count());
        taskDeque.add(task);
        return task.result();
    }

    public List<?> transaction(List<DequeTask<?>> tasks) {
        checkTaskQueueConsumer();
        DequeTask<? extends List<?>> task = DequeTask.of(() -> tasks.stream().map(DequeTask::get).toList());
        taskDeque.add(task);
        return task.result();
    }
    public List<HValue> getByKeyword(String pattern) {
        checkTaskQueueConsumer();
        DequeTask<List<HValue>> task = DequeTask.of(() -> table.values().parallelStream()
                .filter(v -> v.key().getName().matches(pattern))
                .toList());
        taskDeque.add(task);
        return task.result();
    }
    public DataType type(String key) {
        checkTaskQueueConsumer();
        DequeTask<DataType> task = DequeTask.of(() -> {
            HValue hValue = table.get(key);
            return DataType.typeOf(hValue.data());
        });
        taskDeque.add(task);
        return task.result();
    }
    public DataType type(HKey key) {
        return type(key.getName());
    }

    public CompletableFuture<Boolean> save(){
        return AsyncService.submit(()->{
            PersistentService persistentService = HashDBMSApp.ctx().getBean(PersistentService.class);
            persistentService.persist(this);
            return true;
        });
    }
    public boolean saveSync() {
        checkTaskQueueConsumer();
        DequeTask<Boolean> task = DequeTask.of(() -> {
            PersistentService persistentService = HashDBMSApp.ctx().getBean(PersistentService.class);
            persistentService.persist(this);
            return true;
        });
        taskDeque.add(task);
        return task.result();
    }
    public void checkTaskQueueConsumer(){
        if (receiveNewTask.get()) {
            return;
        }
        throw new ServiceStoppedException("database '"+info.getName()+"' has stopped providing external services");
    }
    @NotNull
    @Override
    public Iterator<HValue> iterator() {
        return table.values().iterator();
    }
}
