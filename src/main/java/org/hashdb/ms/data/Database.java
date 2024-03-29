package org.hashdb.ms.data;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.exception.LikePatternSyntaxException;
import org.hashdb.ms.exception.IllegalJavaClassStoredException;
import org.hashdb.ms.exception.IncreaseUnsupportedException;
import org.hashdb.ms.exception.ServiceStoppedException;
import org.hashdb.ms.persistent.aof.AofFlusher;
import org.hashdb.ms.persistent.hdb.AbstractHdb;
import org.hashdb.ms.util.BlockingQueueTaskConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

/**
 * Date: 2023/11/21 1:45
 * java.util.PriorityQueue 优先队列
 * ThreeSet 有序集合， 可比较
 * BitSet 位集 => BitMap
 *
 * @author Huanyu Mark
 */
@Slf4j
public class Database extends BlockingQueueTaskConsumer implements
        Iterable<HValue<?>>, IDatabase, AutoCloseable, ApplicationContextAware {

    /**
     * 数据库信息
     */
    private final DatabaseInfos info;
    /**
     * 可以用 {@link String} 来当作键名来查询， 原因参见 {@link #get(String key)}
     * 因为一切操作都需要通过opsEventQueue 来进行，且只有一个消费者线程，所以天生线程安全，故而使用
     * {@link HashMap}
     */
    protected HashMap<String, HValue<?>> table;
    //    protected final AtomLazy<ScheduledFuture<?>> saveTask;
    public final Object SAVE_TASK_LOCK = new Object();
    private final AtomicInteger usingCount = new AtomicInteger(0);
    // TODO: 2024/1/15  给用户提供一个选项, 设置读操作多线程化阈值(负数为不使用)与设置读操作时的并行度
    private final List<OpsTask<?>> readTaskBatch = new ArrayList<>();

    //    @StaticAutowired
//    private static PersistentService persistentService;
    private AbstractHdb hdb;

    private AofFlusher aofFlusher;

//    {
//        if (hdbManager != null) {
//            hdb = hdbManager.get(this);
//        } else {
//            hdb = nopHdb;
//        }
//        if (aofManager != null) {
//            aofFlusher = aofManager.get(this);
//        } else {
//            aofFlusher = nopAofFlusher;
//        }
//    }

    @Override
    protected void exeTask(BlockingDeque<OpsTask<?>> taskDeque) throws InterruptedException {
        // TODO: 2024/1/15  给用户提供一个选项, 选择是否使用读操作多线程化阈值(负数为不使用)与设置读操作时的并行度, 即下面TODO提及的阈值
        // 判断任务的读写属性, 如果是读,则先收集然后用并行流处理,否则挨个处理
        OpsTask<?> task = taskDeque.take();
        while (task.isRead()) {
            readTaskBatch.add(task);
            // 提前检查避免一直阻塞在迭代末尾的take上
            if (taskDeque.isEmpty()) {
                break;
            }
            task = taskDeque.take();
        }
        // 执行读任务
        // TODO: 2024/1/15  这里选择串/并行的标准太过草率, 不要用任务多少来比较线程切换/串行的时间开销
        // 最好是在编译期将命令的执行期望消耗时间进行累加,然后与一个阈值进行比较,这样很能得到正确的结果
        if (readTaskBatch.size() > 10) {
            // 如果读任务多, 则并行执行, 线程切换开销可以被覆盖
            readTaskBatch.parallelStream().forEach(OpsTask::get);
        } else {
            // 读任务过少, 则串行执行, 否则线程切换开销大于串行执行
            for (OpsTask<?> opsTask : readTaskBatch) {
                opsTask.get();
            }
        }
        readTaskBatch.clear();
        // 执行写任务
        if (!task.isRead()) {
            task.get();
        }
    }

    public void retain() {
        usingCount.incrementAndGet();
    }

    public void release() {
        usingCount.decrementAndGet();
    }

    public int getUsingCount() {
        return usingCount.get();
    }

    public CompletableFuture<Boolean> startDaemon() {
//        saveTask.get();
        return startConsumeOpsTask();
    }

//    public synchronized boolean stopSaveTask() {
//        if (!saveTask.isResolved()) {
//            return true;
//        }
//        // 如果在保存任务中，执行线程被阻塞在IO,网络操作中，则不直接
//        // 抛出中断异常， 让执行线程继续执行
//        saveTask.get().cancel(false);
//        saveTask.computedWith(null);
//        return true;
//    }

    Database(DatabaseInfos databaseInfos) {
        this.info = databaseInfos;
        this.table = new HashMap<>();
//        saveTask = AtomLazy.of(() -> {
//            final long nextSaveTime = hdbConfig.getSaveInterval() + info.getLastSaveTime().getTime();
//            long initDelay = nextSaveTime - System.currentTimeMillis();
//            if (initDelay < 0) {
//                initDelay += hdbConfig.getSaveInterval();
//            }
//            return AsyncService.setInterval(() -> {
//                persistentService.persist(this);
//            }, hdbConfig.getSaveInterval(), initDelay);
//        });
    }

//    @Deprecated
//    public Database(DatabaseInfos databaseInfos, @NotNull Map<String, ? extends StorableHValue<?>> fromData
//    ) {
//        this(databaseInfos);
//        restoreAll(fromData);
//    }

    public void unsafeSetData(HashMap<String, HValue<?>> data) {
        if (this.table != null) {
            this.table.clear();
        }
        this.table = Objects.requireNonNull(data);
        opsTaskDeque.clear();
    }


    private void restoreAll(Map<String, ? extends StorableHValue<?>> from) {
        for (var entry : from.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            Long expireMillis = null;
            if (value.expireDate() != null) {
                expireMillis = value.expireDate().getTime() - System.currentTimeMillis();
                if (expireMillis < 0) {
                    continue;
                }
            }
            HValue<?> hValue = new HValue<>(key, value.data()).
                    clearBy(this, expireMillis, value.deletePriority());
            table.put(key, hValue);
        }
    }

    public DatabaseInfos getInfos() {
        return info;
    }

    /**
     * @param key      键名
     * @param value    值
     * @param millis   过期时间
     * @param priority 删除优先级
     * @return 旧值
     */
    public @Nullable HValue<?> set(String key, Object value, Long millis, OpsTaskPriority priority) {
        HValue<Object> hValue = new HValue<>(key, value).clearBy(this, millis, priority);
        HValue<?> oldValue = table.put(key, hValue);
        if (oldValue != null) {
            oldValue.cancelClear();
        }
        hdb.modify();
        return oldValue;
    }

    /**
     * 等效于命令：
     * INC $KEY [,$STEP][, --expire=MILLIS] … $KEY[,$STEP][,--expire=MILLIS]
     *
     * @param key      键名
     * @param step     递增步长， 可以为负数
     * @param millis   几毫秒后过期, 如果 INC 的键不存在，则将其设置为该新键的过期时间
     * @param priority 删除优先级
     * @return 旧值
     */
    public HValue<?> inc(String key, String step, Long millis, OpsTaskPriority priority) {
        Number stepNumber;
        try {
            if (step.contains(".")) {
                stepNumber = Double.valueOf(step);
            } else {
                stepNumber = Long.valueOf(step);
            }
        } catch (NumberFormatException e) {
            throw new IncreaseUnsupportedException(STR."step '\{step}' must be a number");
        }
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
                hdb.modify();
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
                hdb.modify();
                return oldValue;
            }
            case NULL -> {
                table.put(key, new HValue<>(key, stepNumber).clearBy(this, millis, priority));
                hdb.modify();
                return null;
            }
            default ->
                    throw new IncreaseUnsupportedException(STR."can`t increase type: '\{dataType}' with step '\{step}'");
        }
    }

    @Nullable
    public HValue<?> get(String key) {
        return table.get(key);
    }

    public List<HValue<?>> getLike(Pattern pattern, Long limit) {
        Stream<HValue<?>> stream = table.values().parallelStream().filter(v -> {
            try {
                return pattern.matcher(v.key()).matches();
            } catch (PatternSyntaxException e) {
                throw new LikePatternSyntaxException(e);
            }
        });
        if (limit == null) {
            return stream.toList();
        }
        return stream.limit(limit).toList();
    }

    public Set<String> keys() {
        return table.keySet();
    }

    /**
     * 等效于命令：
     * VALUES $LIMIT
     * 获取数据库中所有值, 并限定数量
     *
     * @param limit 限定数量
     */

    public List<HValue<?>> values(int limit) {
        return table.values().stream().limit(limit).toList();
    }

    /**
     * 等效于命令：
     * VALUES
     * 获取数据库中所有值
     */
    public Collection<HValue<?>> values() {
        return table.values();
    }

    /**
     * 等效于命令：
     * DEL $KEY ... $KEY
     *
     * @param key 键名
     */
    public HValue<?> del(String key) {
        HValue<?> value = table.remove(key);
        if (value != null) {
            value.cancelClear();
        }
        hdb.modify();
        return value;
    }


    /**
     * 等效于命令：
     * CLEAR
     */
    public void clear() {
        table.values().forEach(HValue::cancelClear);
        hdb.modify(table.size());
        table.clear();
    }

    /**
     * 等效于 {@link #clear()}
     */
    public void flush() {
        clear();
    }

    /**
     * 等效于命令：
     * DEL LIKE $PATTERN [$LIMIT]
     *
     * @param pattern 正则表达式
     */
    public List<HValue<?>> delLike(Pattern pattern, @Nullable Long limit) {
        // 如果在filter后在peek里删除键, 会影响原集合, 在遍历时,改变原集合的结构, 会报并发修改异常
        List<HValue<?>> matchedValues = table.values().parallelStream()
                .filter(value -> {
                    try {
                        return pattern.matcher(value.key()).matches();
                    } catch (PatternSyntaxException e) {
                        throw new LikePatternSyntaxException(e);
                    }
                }).toList();
        matchedValues.parallelStream().forEach(v -> table.remove(v.key()).cancelClear());
        if (limit == null) {
            return matchedValues;
        }
        hdb.modify(matchedValues.size());
        return matchedValues.stream().limit(limit).toList();
    }

    /**
     * 等效于命令：
     * RPL $KEY $VALUE [,--expire=$MILLIS]... $KEY $VALUE [,--expire=$MILLIS]
     * 如果键名不存在，则返回null，如果键名存在，则返回替换出的旧值
     *
     * @param key      键名
     * @param value    新值
     * @param millis   几毫秒后过期
     * @param priority 操作优先级
     * @return 旧值，可能为空
     */
    public HValue<?> rpl(String key, Object value, Long millis, OpsTaskPriority priority) {
        @SuppressWarnings("unchecked")
        HValue<Object> hValue = (HValue<Object>) table.get(key);
        if (hValue == null) {
            return null;
        }
        HValue<Object> old = hValue.cloneDefault();
        hValue.clearBy(this, millis, priority);
        hValue.data(value);
        hdb.modify();
        return old;
    }

    /**
     * 等效于命令：
     * EXISTS $KEY1 $KEY2 ...
     *
     * @param keys 键名序列
     * @return table里存在的键名 在keys序列里的索引
     */
    public List<Long> exists(List<String> keys) {
        long[] index = {0L};
        List<Long> containIndexes = new ArrayList<>();
        keys.forEach(key -> {
            if (table.containsKey(key)) {
                containIndexes.add(index[0]);
            }
            ++index[0];
        });
        return containIndexes;
    }

    public boolean exists(String key) {
        return table.containsKey(key);
    }

    /**
     * 等效于命令：
     * EXPIRE $KEY $MILLIS
     * null 不过期
     *
     * @param key      要过期的键
     * @param millis   多少毫秒后过期
     * @param priority 操作优先级
     */
    public void expire(String key, Long millis, OpsTaskPriority priority) {
        HValue<?> value = table.get(key);
        if (value == null) {
            return;
        }
        value.clearBy(this, millis, priority);
        hdb.modify();
    }

    /**
     * 等效于命令：
     * EXPIREAT $KEY $TIMESTAMP
     *
     * @param key       要过期的键
     * @param timestamp 在该时间戳过期
     * @param priority  操作优先级
     */
    @Deprecated
    public void expireAt(String key, long timestamp, OpsTaskPriority priority) {
        HValue<?> value = table.get(key);
        if (value == null) {
            return;
        }
        value.clearBy(this, timestamp, priority);
        hdb.modify();
    }

    /**
     * 等效于命令：
     * EXPIRE LIKE $PATTERN $MILLIS
     * millis 为 null 不过期
     *
     * @param pattern  正则
     * @param millis   多少毫秒后过期
     * @param priority 操作优先级
     */
    public void expireLike(Pattern pattern, Long millis, OpsTaskPriority priority) {
        table.values().parallelStream()
                .filter(v -> {
                    try {
                        return pattern.matcher(v.key()).matches();
                    } catch (PatternSyntaxException e) {
                        throw new LikePatternSyntaxException(e);
                    }
                })
                .forEach(v -> {
                    v.clearBy(this, millis, priority);
                    hdb.modify();
                });
    }

    /**
     * 等效于命令：
     * EXPIREAT LIKE $PATTERN $TIMESTAMP
     *
     * @param pattern   正则
     * @param timestamp 到这个时间戳过期
     * @param priority  操作优先级
     */
    public void expireAtLike(Pattern pattern, long timestamp, OpsTaskPriority priority) {
        table.values().parallelStream()
                .filter(v -> {
                    try {
                        return pattern.matcher(v.key()).matches();
                    } catch (PatternSyntaxException e) {
                        throw new LikePatternSyntaxException(e);
                    }
                })
                .forEach(v -> {
                    v.clearBy(this, timestamp, priority);
                    hdb.modify();
                });
    }

    /**
     * 等效于命令：
     * COUNT
     */
    public int count() {
        return table.size();
    }

    /**
     * 等效于命令：
     * KEY LIKE pattern
     *
     * @param pattern 正则表达式
     */
    public long countLike(Pattern pattern) {
        return table.values().parallelStream()
                .filter(v -> {
                    try {
                        return pattern.matcher(v.key()).matches();
                    } catch (PatternSyntaxException e) {
                        throw new LikePatternSyntaxException(e);
                    }
                })
                .count();
    }

    /**
     * 开启事务，确保一系列任务的原子操作
     * 将一个序列的任务包装入一个任务中提交
     *
     * @param tasks 序列任务
     */
    public List<?> transactional(List<OpsTask<?>> tasks) {
        return tasks.stream().map(OpsTask::get).toList();
    }

    /**
     * 等效于命令：
     * TYPE $KEY
     *
     * @param key 键名
     * @return 数据类型
     */
    public DataType type(String key) {
        HValue<?> hValue = table.get(key);
        return DataType.typeofHValue(hValue);
    }

    /**
     * 等效于命令：
     *
     * @param key 键名
     * @return -2 键不存在
     * -1 键不会过期
     * >=0 键剩余时间
     */
    public Long ttl(String key) {
        HValue<?> value = table.get(key);
        return value == null ? -2 : value.ttl();
    }

    public CompletableFuture<Boolean> save() {
        var res = new CompletableFuture<Boolean>();
        var f1 = aofFlusher.flush();
        var f2 = hdb.flush();
        BiFunction<Boolean, Throwable, ?> handler = (ok, e) -> {
            if (e != null) {
                res.completeExceptionally(e);
                return null;
            }
            res.complete(ok);
            return null;
        };
        f1.handleAsync(handler);
        f2.handleAsync(handler);
        return res;
    }

    /**
     * 等效于命令：
     * SAVE
     * 异步保存数据库， 步入事件循环
     *
     * @return 异步结果
     */
    public boolean saveSync() {
        try {
            var f1 = aofFlusher.flush();
            var f2 = hdb.flush();
            if (!f1.join()) {
                return false;
            }
            return f2.join();
        } catch (Exception e) {
            log.error("saveSync error", e);
            return false;
        }
//        persistentService.persist(this);
    }

    @NotNull
    @Override
    public Iterator<HValue<?>> iterator() {
        return table.values().iterator();
    }

    @Override
    public boolean checkTaskQueueConsumer() {
        if (super.checkTaskQueueConsumer()) {
            return true;
        }
        throw new ServiceStoppedException(STR."database '\{info.getName()}' has stopped providing external services");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Database db)) return false;

        return Objects.equals(info, db.info);
    }

    @Override
    public int hashCode() {
        return info != null ? info.hashCode() : 0;
    }

    @Override
    public String toString() {
        return STR."Database\{info}";
    }

    @Override
    public void close() throws Exception {
        var f1 = stopConsumeOpsTask();
        hdb.close();
        aofFlusher.close();
        f1.join();
        //        stopSaveTask();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        hdb = applicationContext.getBeanProvider(AbstractHdb.class).getObject(this);
        aofFlusher = applicationContext.getBeanProvider(AofFlusher.class).getObject(this);
    }

//    /**
//     * @return 如果启用了aof持久化功能, 则返回aofFile
//     * 若aofFile在硬盘上不存在, 则创建并写入现在数据库中
//     * 存有的k-v
//     */
//    public AofFile getBaseFile() {
//        if (!aofConfig.isEnabled()) {
//            return null;
//        }
//        if (aofBaseFile != null) {
//            return aofBaseFile;
//        }
//        File aofFileRoot = aofConfig.getRootDir();
//        File aofBaseFile = new File(aofFileRoot, aofConfig.getAofBaseFileName());
//        if (aofBaseFile.exists() || !aofBaseFile.mkdir()) {
//            throw Exit.error()
//        }
//    }
}
