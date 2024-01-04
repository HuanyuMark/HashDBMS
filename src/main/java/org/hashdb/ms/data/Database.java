package org.hashdb.ms.data;

import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.config.HdbConfig;
import org.hashdb.ms.exception.IllegalJavaClassStoredException;
import org.hashdb.ms.exception.IncreaseUnsupportedException;
import org.hashdb.ms.exception.LikePatternSyntaxException;
import org.hashdb.ms.exception.ServiceStoppedException;
import org.hashdb.ms.persistent.PersistentService;
import org.hashdb.ms.util.AsyncService;
import org.hashdb.ms.util.BlockingQueueTaskConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
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
public class Database extends BlockingQueueTaskConsumer implements Iterable<HValue<?>>, IDatabase {

    /**
     * 数据库信息
     */
    private final DatabaseInfos info;
    /**
     * 可以用 {@link String} 来当作键名来查询， 原因参见 {@link #get(String key)}
     * 因为一切操作都需要通过opsEventQueue 来进行，且只有一个消费者线程，所以天生线程安全，故而使用
     * {@link HashMap}
     */
    protected final HashMap<String, HValue<?>> table = new HashMap<>();
    protected final AtomicReference<ScheduledFuture<?>> saveTask = new AtomicReference<>();
    public final Object SAVE_TASK_LOCK = new Object();

    private final AtomicInteger usingCount = new AtomicInteger(0);

    public void use() {
        usingCount.incrementAndGet();
    }

    public void release() {
        usingCount.decrementAndGet();
    }

    public int getUsingCount() {
        return usingCount.get();
    }

    public CompletableFuture<Boolean> startDaemon() {
        startSaveTask();
        return startConsumeOpsTask();
    }

    public boolean startSaveTask() {
        Supplier<ScheduledFuture<?>> saveTaskSupplier = () -> {
            HdbConfig HDBConfig = HashDBMSApp.ctx().getBean(HdbConfig.class);
            PersistentService persistentService = HashDBMSApp.ctx().getBean(PersistentService.class);
            final long nextSaveTime = HDBConfig.getSaveInterval() + info.getLastSaveTime().getTime();
            long initDelay = nextSaveTime - System.currentTimeMillis();
            if (initDelay < 0) {
                initDelay += HDBConfig.getSaveInterval();
            }
            return AsyncService.setInterval(() -> {
                persistentService.persist(this);
            }, HDBConfig.getSaveInterval(), initDelay);
        };

        saveTask.compareAndSet(null, saveTaskSupplier.get());
        return true;
    }

    public synchronized boolean stopSaveTask() {
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
    }

    public Database(int id, String name, Date createTime, @NotNull Map<String, StorableHValue<?>> fromData) {
        this(new DatabaseInfos(id, name, createTime), fromData);
    }

    public Database(DatabaseInfos databaseInfos, @NotNull Map<String, StorableHValue<?>> fromData) {
        this.info = databaseInfos;
        restoreAll(fromData);
    }

    private Database restoreAll(Map<String, StorableHValue<?>> from) {
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
        return this;
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
            throw new IncreaseUnsupportedException("step '" + step + "' must be a number");
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
                table.put(key, new HValue<>(key, stepNumber).clearBy(this, millis, priority));
                return null;
            }
            default ->
                    throw new IncreaseUnsupportedException("can`t increase type: '" + dataType + "' with step '" + step + "'");
        }
    }


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
        if (limit != null) {
            return stream.limit(limit).toList();
        }
        return stream.toList();
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
        return value;
    }


    /**
     * 等效于命令：
     * CLEAR
     */
    public void clear() {
        table.values().forEach(HValue::cancelClear);
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
     * @param priority
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
     * @param priority
     */
    public void expire(String key, Long millis, OpsTaskPriority priority) {
        HValue<?> value = table.get(key);
        if (value == null) {
            return;
        }
        value.clearBy(this, millis, priority);
    }

    /**
     * 等效于命令：
     * EXPIREAT $KEY $TIMESTAMP
     *
     * @param key       要过期的键
     * @param timestamp 在该时间戳过期
     * @param priority
     */
    @Deprecated
    public void expireAt(String key, long timestamp, OpsTaskPriority priority) {
        HValue<?> value = table.get(key);
        if (value == null) {
            return;
        }
        value.clearBy(this, timestamp, priority);
    }

    /**
     * 等效于命令：
     * EXPIRE LIKE $PATTERN $MILLIS
     * millis 为 null 不过期
     *
     * @param pattern  正则
     * @param millis   多少毫秒后过期
     * @param priority
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
                });
    }

    /**
     * 等效于命令：
     * EXPIREAT LIKE $PATTERN $TIMESTAMP
     *
     * @param pattern   正则
     * @param timestamp 到这个时间戳过期
     * @param priority
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
        return AsyncService.submit(() -> {
            PersistentService persistentService = HashDBMSApp.ctx().getBean(PersistentService.class);
            persistentService.persist(this);
            return true;
        });
    }

    /**
     * 等效于命令：
     * SAVE
     * 异步保存数据库， 步入事件循环
     *
     * @return 异步结果
     */
    public boolean saveSync() {
        PersistentService persistentService = HashDBMSApp.ctx().getBean(PersistentService.class);
        persistentService.persist(this);
        return true;
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
        throw new ServiceStoppedException("database '" + info.getName() + "' has stopped providing external services");
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
        return "Database" + info;
    }
}
