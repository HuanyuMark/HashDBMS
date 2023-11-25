package org.hashdb.ms.data;

import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.config.DBFileConfig;
import org.hashdb.ms.exception.IllegalJavaClassStoredException;
import org.hashdb.ms.exception.IncreaseUnsupportedException;
import org.hashdb.ms.exception.ServiceStoppedException;
import org.hashdb.ms.exception.WorkerInterruptedException;
import org.hashdb.ms.persistent.PersistentService;
import org.hashdb.ms.util.AsyncService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Date: 2023/11/21 1:45
 * java.util.PriorityQueue 优先队列
 * ThreeSet 有序集合， 可比较
 * BitSet 位集 => BitMap
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class Database implements Iterable<HValue<?>> {
    private final DatabaseInfos info;
    /**
     * 可以用 {@link String} 来当作键名来查询， 原因参见 {@link #get(String key)}
     * 因为一切操作都需要通过opsEventQueue 来进行，且只有一个消费者线程，所以天生线程安全，故而使用
     * {@link HashMap}
     */
    protected final HashMap<String, HValue<?>> table = new HashMap<>();
    protected final AtomicReference<ScheduledFuture<?>> saveTask = new AtomicReference<>();
    protected final AtomicReference<CompletableFuture<?>> opsTaskDequeConsumeLoopConsumer = new AtomicReference<>();
    protected final AtomicBoolean receiveNewTask = new AtomicBoolean(false);
    protected final BlockingDeque<OpsTask<?>> opsTaskDeque = new LinkedBlockingDeque<>();
    public final Object SAVE_TASK_LOCK = new Object();

    protected void startDatabaseDaemonTask() {
        startSaveTask();
        startOpsTaskDequeConsumer();
    }

    public CompletableFuture<Boolean> startOpsTaskDequeConsumer() {
        // 正在接收新任务，则直接 返回启动成功
        if (receiveNewTask.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(true);
        }
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Supplier<CompletableFuture<?>> taskDequeConsumeLoopConsumerSupplier = () -> AsyncService.submit(() -> {
            future.complete(true);
            while (true) {
                if (opsTaskDeque.size() == 0 && receiveNewTask.compareAndSet(false, false)) {
                    opsTaskDequeConsumeLoopConsumer.set(null);
                    break;
                }
                OpsTask<?> task;
                try {
                    task = opsTaskDeque.take();
                } catch (InterruptedException e) {
                    throw new WorkerInterruptedException(e);
                }
                task.get();
            }
        });
        // 需要接收新任务
        // 消费线程在running, 就不开启新消费任务, 返回启动成功
        // 不在running, 就开启一个新消费任务
        if (opsTaskDequeConsumeLoopConsumer.compareAndSet(null, taskDequeConsumeLoopConsumerSupplier.get())) {
            return future;
        }
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> stopOpsTaskDequeConsumer() {
        if (!receiveNewTask.compareAndSet(true, false)) {
            return CompletableFuture.completedFuture(true);
        }
        // 避免消费者线程一直卡在 take() 方法
        opsTaskDeque.add(OpsTask.empty());
        CompletableFuture<?> consumeLoopResult = opsTaskDequeConsumeLoopConsumer.get();
        if (consumeLoopResult == null) {
            return CompletableFuture.completedFuture(true);
        }
        return consumeLoopResult.thenApply(v -> true);
    }

    public boolean startSaveTask() {
        Supplier<ScheduledFuture<?>> saveTaskSupplier = () -> {
            DBFileConfig dbFileConfig = HashDBMSApp.ctx().getBean(DBFileConfig.class);
            PersistentService persistentService = HashDBMSApp.ctx().getBean(PersistentService.class);
            final long nextSaveTime = dbFileConfig.getSaveInterval() + info.getLastSaveTime().getTime();
            long initDelay = nextSaveTime - System.currentTimeMillis();
            if (initDelay < 0) {
                initDelay += dbFileConfig.getSaveInterval();
            }
            return AsyncService.setInterval(() -> {
                persistentService.persist(this);
            }, dbFileConfig.getSaveInterval(), initDelay);
        };

        saveTask.compareAndSet(null, saveTaskSupplier.get());
        return true;
    }

    public boolean stopSaveTask() {
        if (saveTask.get() == null) {
            return true;
        }
        // 如果在保存任务中，执行线程被阻塞在IO,网络操作中，则不直接
        // 抛出中断异常， 让执行线程继续执行
        saveTask.get().cancel(false);
        saveTask.set(null);
        return true;
    }

    public Database(int id, String name, Date createTime) {
        this(new DatabaseInfos(id, name, createTime));
    }

    public Database(DatabaseInfos databaseInfos) {
        this.info = databaseInfos;
        startDatabaseDaemonTask();
    }

    public Database(int id, String name, Date createTime, @NotNull Map<String, Object> initialValues) {
        this(new DatabaseInfos(id, name, createTime), initialValues);
    }

    public Database(DatabaseInfos databaseInfos, @NotNull Map<String, Object> initialValues) {
        this.info = databaseInfos;
        initialValues.forEach((k, v) -> table.put(k, new HValue<>(k, v)));
        startDatabaseDaemonTask();
    }

    public DatabaseInfos getInfos() {
        return info;
    }

    public @Nullable HValue<?> set(String key, Long millis, Object value) {
        return submitOpsTaskAsync(setTask(key, millis, value));
    }

    /**
     * 等效于命令：
     * SET $KEY [#TYPE_SYMBOL,]$VALUE[,--[h]expire=$MILLIS] ... $KEY [#TYPE_SYMBOL,]$VALUE[,--[h]expire=$MILLIS]
     *
     * @param key    键名
     * @param millis 过期时间
     * @param value  值
     * @return 旧值
     */
    public OpsTask<@Nullable HValue<?>> setTask(String key, Long millis, Object value) {
        return OpsTask.of(() -> {
            HValue<Object> hValue = new HValue<>(key, value).clearBy(this, millis);
            HValue<?> oldValue = table.put(key, hValue);
            if (oldValue != null) {
                oldValue.cancelClear();
            }
            return oldValue;
        });
    }

    public HValue<?> inc(String key, Long millis, String step) {
        return submitOpsTaskAsync(incTask(key, millis, step));
    }

    /**
     * 等效于命令：
     * INC $KEY [,$STEP][, --expire=MILLIS] … $KEY[,$STEP][,--expire=MILLIS]
     *
     * @param key    键名
     * @param millis 几毫秒后过期, 如果 INC 的键不存在，则将其设置为该新键的过期时间
     * @param step   递增步长， 可以为负数
     * @return 旧值
     */
    public OpsTask<@Nullable HValue<?>> incTask(String key, Long millis, String step) {
        Number stepNumber;
        try {
            if (step.contains(".")) {
                stepNumber = Double.valueOf(step);
            } else {
                stepNumber = Long.valueOf(step);
            }
        } catch (NumberFormatException e) {
            throw new IncreaseUnsupportedException("step '" + step + "' must be a number");
        }

        return OpsTask.of(() -> {
            @SuppressWarnings("unchecked")
            HValue<Object> value = (HValue<Object>) table.get(key);
            final DataType dataType = DataType.typeofHValue(value);
            switch (dataType) {
                case STRING -> {
                    final HValue<Object> oldValue = value.cloneDefault();
                    final String rawValue = ((String) value.data());
                    final Number newValue;
                    try {
                        if (rawValue.contains(".")) {
                            newValue = Double.parseDouble(rawValue) + ((stepNumber instanceof Double) ? (Double) stepNumber : (Long) stepNumber);
                        } else {
                            newValue = Long.parseLong(rawValue) + ((stepNumber instanceof Double) ? (Double) stepNumber : (Long) stepNumber);
                        }
                    } catch (NumberFormatException e) {
                        throw new IncreaseUnsupportedException("can`t increase string: '" + rawValue + "' with step '" + step + "'");
                    } catch (ClassCastException e) {
                        throw IllegalJavaClassStoredException.of(rawValue.getClass());
                    }
                    value.data(newValue);
                    return oldValue;
                }
                case NUMBER -> {
                    final HValue<Object> oldValue = value.cloneDefault();
                    final Number rawValue = (Number) value.data();
                    final Number newValue;
                    try {
                        newValue = (rawValue instanceof Long ? (Long) rawValue : (Double) rawValue) + (stepNumber instanceof Double ? (Double) stepNumber : (Long) stepNumber);
                    } catch (NumberFormatException e) {
                        throw new IncreaseUnsupportedException("can`t increase number: '" + rawValue + "' with step '" + step + "'");
                    } catch (ClassCastException e) {
                        throw IllegalJavaClassStoredException.of(rawValue.getClass());
                    }
                    value.data(newValue);
                    return oldValue;
                }
                case NULL -> {
                    table.put(key, new HValue<>(key, stepNumber).clearBy(this, millis));
                    return null;
                }
                default ->
                        throw new IncreaseUnsupportedException("can`t increase type: '" + dataType + "' with step '" + step + "'");
            }
        });
    }


    public HValue<?> get(String key) {
        return submitOpsTaskAsync(getTask(key));
    }

    public List<HValue<?>> getLike(String pattern, int limit) {
        return submitOpsTaskAsync(getLikeTask(pattern, limit));
    }

    /**
     * 等效于命令：
     * GET $KEY … $KEY
     *
     * @param key 键名
     */
    public OpsTask<HValue<?>> getTask(String key) {
        return OpsTask.of(() -> table.get(key));
    }

    /**
     * 等效于命令：
     * GET LIKE $PATTERN1 [,$LIMIT]
     *
     * @param pattern 正则
     * @param limit   最多返回多少个
     * @return 对应的value序列
     */
    public OpsTask<List<HValue<?>>> getLikeTask(String pattern, Integer limit) {
        return OpsTask.of(() -> {
            Stream<HValue<?>> stream = table.values().parallelStream().filter(v -> v.key().matches(pattern));
            if(limit != null) {
                return stream.limit(limit).toList();
            }
            return stream.toList();
        });
    }

    public List<String> keys(int limit) {
        return submitOpsTaskAsync(keysTask(limit));
    }


    public Set<String> keys() {
        return submitOpsTaskAsync(keysTask());
    }

    /**
     * 等效于命令:
     * KEYS $LIMIT
     */
    public OpsTask<List<String>> keysTask(int limit) {
        return OpsTask.of(() -> table.keySet().stream().limit(limit).toList());
    }

    /**
     * 等效于命令:
     * KEYS
     */
    public OpsTask<Set<String>> keysTask() {
        return OpsTask.of(table::keySet);
    }


    public List<HValue<?>> values(int limit) {
        return submitOpsTaskAsync(valuesTask(limit));
    }


    public Collection<HValue<?>> values() {
        return submitOpsTaskAsync(valuesTask());
    }

    /**
     * 等效于命令：
     * VALUES $LIMIT
     * 获取数据库中所有值, 并限定数量
     *
     * @param limit 限定数量
     */
    public OpsTask<List<HValue<?>>> valuesTask(int limit) {
        return OpsTask.of(() -> table.values().stream().limit(limit).toList());
    }

    /**
     * 等效于命令：
     * VALUES
     * 获取数据库中所有值
     */
    public OpsTask<Collection<HValue<?>>> valuesTask() {
        return OpsTask.of(table::values);
    }

    public HValue<?> del(String key) {
        return submitOpsTaskAsync(delTask(key));
    }

    /**
     * 等效于命令：
     * DEL $KEY ... $KEY
     *
     * @param key 键名
     */
    public OpsTask<HValue<?>> delTask(String key) {
        return OpsTask.of(() -> {
            HValue<?> value = table.remove(key);
            if (value != null) {
                value.cancelClear();
            }
            return value;
        });
    }

    public void clear() {
        submitOpsTaskAsync(clearTask());
    }

    /**
     * 等效于命令：
     * CLEAR
     */
    public OpsTask<Boolean> clearTask() {
        return OpsTask.of(() -> {
            table.values().forEach(HValue::cancelClear);
            table.clear();
            return true;
        });
    }

    /**
     * 等效于 {@link #clearTask()}
     */
    public OpsTask<Boolean> flushTask(){
        return clearTask();
    }

    public List<HValue<?>> delLike(String pattern, @Nullable Long limit) {
        return submitOpsTaskAsync(delLikeTask(pattern, limit));
    }

    /**
     * 等效于命令：
     * DEL LIKE $PATTERN [$LIMIT]
     *
     * @param pattern 正则表达式
     */
    public OpsTask<List<HValue<?>>> delLikeTask(String pattern, @Nullable Long limit) {
        return OpsTask.of(() -> {
                    Stream<HValue<?>> stream = table.values().parallelStream()
                            .filter(value -> value.key().matches(pattern))
                            .peek(v -> table.remove(v.key()).cancelClear());
                    if (limit == null) {
                        return stream.toList();
                    }
                    return stream.limit(limit).toList();
                }
        );
    }

    public HValue<?> rpl(String key, Long millis, Object value) {
        return submitOpsTaskAsync(rplTask(key, millis, value));
    }

    /**
     * 等效于命令：
     * RPL $KEY $VALUE [,--expire=$MILLIS]... $KEY $VALUE [,--expire=$MILLIS]
     * 如果键名不存在，则返回null，如果键名存在，则返回替换出的旧值
     *
     * @param key    键名
     * @param millis 几毫秒后过期
     * @param value  新值
     * @return 旧值，可能为空
     */
    public OpsTask<HValue<?>> rplTask(String key, Long millis, Object value) {
        return OpsTask.of(() -> {
            @SuppressWarnings("unchecked")
            HValue<Object> hValue = (HValue<Object>) table.get(key);
            if (hValue == null) {
                return null;
            }
            HValue<Object> old = hValue.cloneDefault();
            hValue.clearBy(this, millis);
            hValue.data(value);
            return old;
        });
    }

    public List<Long> exists(List<String> keys) {
        return submitOpsTaskAsync(existsTask(keys));
    }

    /**
     * 等效于命令：
     * EXISTS $KEY1 $KEY2 ...
     *
     * @param keys 键名序列
     * @return table里存在的键名 在keys序列里的索引
     */
    public OpsTask<List<Long>> existsTask(List<String> keys) {
        return OpsTask.of(() -> {
            long[] index = {0L};
            List<Long> containIndexes = new ArrayList<>();
            keys.forEach(key -> {
                if (table.containsKey(key)) {
                    containIndexes.add(index[0]);
                }
                ++index[0];
            });
            return containIndexes;
        });
    }


    public void expire(String key, Long millis) {
        submitOpsTaskAsync(expireTask(key, millis));
    }

    /**
     * 等效于命令：
     * EXPIRE $KEY $MILLIS
     * null 不过期
     *
     * @param key    要过期的键
     * @param millis 多少毫秒后过期
     */
    public OpsTask<?> expireTask(String key, Long millis) {
        return OpsTask.of(() -> {
            HValue<?> value = table.get(key);
            if (value == null) {
                return;
            }
            value.clearBy(this, millis);
        });
    }

    public void expireAt(String key, long timestamp) {
        submitOpsTaskAsync(expireAtTask(key, timestamp));
    }

    /**
     * 等效于命令：
     * EXPIREAT $KEY $TIMESTAMP
     *
     * @param key       要过期的键
     * @param timestamp 在该时间戳过期
     */
    public OpsTask<?> expireAtTask(String key, long timestamp) {
        return OpsTask.of(() -> {
            HValue<?> value = table.get(key);
            if (value == null) {
                return;
            }
            value.clearBy(this, timestamp);
        });
    }

    public void expireLike(String pattern, long millis) {
        submitOpsTaskAsync(expireLikeTask(pattern, millis));
    }

    /**
     * 等效于命令：
     * EXPIRE LIKE $PATTERN $MILLIS
     * millis 为 null 不过期
     *
     * @param pattern 正则
     * @param millis  多少毫秒后过期
     */
    public OpsTask<?> expireLikeTask(String pattern, Long millis) {
        return OpsTask.of(() -> {
            table.values().parallelStream()
                    .filter(v -> v.key().matches(pattern))
                    .forEach(v -> {
                        v.clearBy(this, millis);
                    });
        });
    }

    public void expireAtLike(String pattern, long timestamp) {
        submitOpsTaskAsync(expireAtLikeTask(pattern, timestamp));
    }

    /**
     * 等效于命令：
     * EXPIREAT LIKE $PATTERN $TIMESTAMP
     *
     * @param pattern   正则
     * @param timestamp 到这个时间戳过期
     */
    public OpsTask<?> expireAtLikeTask(String pattern, long timestamp) {
        return OpsTask.of(() -> {
            table.values().parallelStream()
                    .filter(v -> v.key().matches(pattern))
                    .forEach(v -> {
                        v.clearBy(this, timestamp);
                    });
        });
    }

    public int count() {
        return table.size();
    }

    /**
     * 等效于命令：
     * COUNT
     */
    public OpsTask<Integer> countTask() {
        return OpsTask.of(table::size);
    }

    public long countLike(String pattern) {
        return submitOpsTaskAsync(countLikeTask(pattern));
    }

    /**
     * 等效于命令：
     * KEY LIKE pattern
     *
     * @param pattern 正则表达式
     */
    public OpsTask<Long> countLikeTask(String pattern) {
        return OpsTask.of(() -> table.values().parallelStream()
                .filter(v -> v.key().matches(pattern))
                .count());
    }

    public List<?> transactional(List<OpsTask<?>> tasks) {
        return submitOpsTaskAsync(transactionalTask(tasks));
    }

    /**
     * 开启事务，确保一系列任务的原子操作
     * 将一个序列的任务包装入一个任务中提交
     *
     * @param tasks 序列任务
     */
    public OpsTask<? extends List<?>> transactionalTask(List<OpsTask<?>> tasks) {
        return OpsTask.of(() -> tasks.stream().map(OpsTask::get).toList());
    }

    public <T> T submitOpsTaskAsync(OpsTask<T> task, OpsTaskPriority priority) {
        checkTaskQueueConsumer();
        if (OpsTaskPriority.LOW == priority) {
            opsTaskDeque.add(task);
        } else {
            opsTaskDeque.addFirst(task);
        }
        return task.result();
    }

    public <T> T submitOpsTaskAsync(OpsTask<T> task) {
        return submitOpsTaskAsync(task, OpsTaskPriority.LOW);
    }


    public DataType type(String key) {
        return submitOpsTaskAsync(typeTask(key));
    }

    /**
     * 等效于命令：
     * TYPE $KEY
     *
     * @param key 键名
     * @return 数据类型
     */
    public OpsTask<DataType> typeTask(String key) {
        return OpsTask.of(() -> {
            HValue<?> hValue = table.get(key);
            return DataType.typeofHValue(hValue);
        });
    }

    public Long ttl(String key) {
        return submitOpsTaskAsync(ttlTask(key));
    }

    /**
     * 等效于命令：
     * TTL $KEY ... $KEY
     *
     * @param key 键名
     * @return 还有多少毫秒过期
     * -2 键不存在
     * -1 键不会过期
     * >=0 键剩余时间
     */
    public OpsTask<Long> ttlTask(String key) {
        return OpsTask.of(() -> {
            HValue<?> value = table.get(key);
            return value == null ? -2 : value.ttl();
        });
    }

    public CompletableFuture<Boolean> save() {
        return AsyncService.submit(() -> {
            PersistentService persistentService = HashDBMSApp.ctx().getBean(PersistentService.class);
            persistentService.persist(this);
            return true;
        });
    }

    public boolean saveSync() {
        return submitOpsTaskAsync(saveSyncTask());
    }

    /**
     * 等效于命令：
     * SAVE
     * 异步保存数据库， 步入事件循环
     *
     * @return 异步结果
     */
    public OpsTask<Boolean> saveSyncTask() {
        return OpsTask.of(() -> {
            PersistentService persistentService = HashDBMSApp.ctx().getBean(PersistentService.class);
            persistentService.persist(this);
            return true;
        });
    }

    public void checkTaskQueueConsumer() {
        if (receiveNewTask.get()) {
            return;
        }
        throw new ServiceStoppedException("database '" + info.getName() + "' has stopped providing external services");
    }

    @NotNull
    @Override
    public Iterator<HValue<?>> iterator() {
        return table.values().iterator();
    }
}
