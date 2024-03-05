package org.hashdb.ms.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.DBRamConfig;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.exception.ServiceStoppedException;
import org.hashdb.ms.support.StaticAutowired;
import org.hashdb.ms.util.AsyncService;
import org.hashdb.ms.util.JsonService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

/**
 * Date: 2023/11/21 2:37
 * 在{@link Database} 中存储 K - V 的pair 以及其对应的上下文
 *
 * @author Huanyu Mark
 */
@Slf4j
@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public class HValue<T> implements Cloneable {
    private static HValue<?> EMPTY;

    private static DBRamConfig dbRamConfig;

    public static HValue<?> empty() {
        return EMPTY;
    }

    @StaticAutowired
    private void loadConfig(DBRamConfig dbRamConfig) {
        HValue.dbRamConfig = dbRamConfig;
        EMPTY = new HValue<>();
    }

    public static <T> T unwrapData(@Nullable HValue<T> hValue) {
        return hValue == null ? null : hValue.data;
    }

    public static long unwrapExpire(@Nullable HValue<?> hValue) {
        return hValue == null ? -2 : hValue.expireMilliseconds == null ? -1 : hValue.expireMilliseconds;
    }

    @Getter
    private final String key;
    @Getter
    private T data;

    @Getter
    private Date modifyTime;

    /**
     * 如果为空，则永不过期
     */
    // TODO: 2024/1/14 可以提供给用户一个默认设置过期时间的选项, 通过设置ExpireTimeStrategy来指定, 固定减少/增加多少,区间,ln增长等等
    @Getter
    @Nullable
    private Long expireMilliseconds;
    @Getter
    @Nullable
    private Date expireDate;

    /**
     * 如果为高,则触发删除任务时, 将删除任务插入任务队列头部, 优先执行
     */
    @Getter
    private OpsTaskPriority deletePriority = dbRamConfig.getExpiredKeyDeletePriority();
    /**
     * 是否取消， 当该key过期时， 清除该key
     */
    private ScheduledFuture<?> clearWhenExpiredTask;

    /**
     * Note: {@link Database}需要主动调用{@link #clearBy(Database)} 来开启过期任务
     * 否则这个{@link #expireDate}不会生效
     */
    public HValue(String key, @NotNull T data, @NotNull Date expireDate, @NotNull OpsTaskPriority deletePriority) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(data);
        this.key = key;
        this.data = data;
        this.expireDate = expireDate;
        this.deletePriority = deletePriority;
    }

    /**
     * 新建一个k-v上下文
     */
    public HValue(String key, @NotNull T data) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(data);
        this.key = key;
        this.data = data;
    }

    private HValue() {
        this.key = null;
        this.data = null;
    }

    public T data() {
        return data;
    }


    public DataType dataType() {
        return DataType.typeofHValue(this);
    }

    public void data(T data) {
        Objects.requireNonNull(data);
        this.data = data;
        this.modifyTime = new Date();
    }

    public String key() {
        return key;
    }


    public long ttl() {
        return expireDate == null ? -1 : expireDate.getTime() - System.currentTimeMillis();
    }

    /**
     * @return 可持久化对象
     * @throws ServiceStoppedException 如果在持久化期间数据库
     */
    public StorableHValue<T> toStorable() throws ServiceStoppedException {
        return new StorableHValue<>(data, expireDate, deletePriority);
    }

    private ScheduledFuture<?> startExpiredTask(Database database, Long expireMilliseconds, @Nullable OpsTaskPriority priority) {
        cancelClear();
        deletePriority = Objects.requireNonNullElse(priority, dbRamConfig.getExpiredKeyDeletePriority());
        return AsyncService.setTimeout(() -> {
            var deleteTask = OpsTask.of(() -> database.del(key));
            if (deletePriority == OpsTaskPriority.HIGH) {
                database.submitOpsTask(deleteTask, OpsTaskPriority.HIGH);
            } else {
                database.submitOpsTask(deleteTask);
            }
        }, expireMilliseconds);
    }

    private void submitDelTaskCurrently(Database database) {
        var deleteTask = OpsTask.of(() -> database.del(key));
        if (deletePriority == OpsTaskPriority.HIGH) {
            database.submitOpsTask(deleteTask, OpsTaskPriority.HIGH);
        } else {
            database.submitOpsTask(deleteTask);
        }
    }

    public HValue<T> clearBy(Database database) {
        if (expireDate != null) {
            long time = expireDate.getTime();
            long remaining = System.currentTimeMillis() - time;
            if (remaining < 0) {
                return this;
            }
            clearWhenExpiredTask = startExpiredTask(database, remaining, deletePriority);
            return this;
        }
        if (expireMilliseconds != null) {
            if (expireMilliseconds == -1) {
                this.expireMilliseconds = null;
                return this;
            }
            if (expireMilliseconds == -2) {
                submitDelTaskCurrently(database);
                return this;
            }
            clearWhenExpiredTask = startExpiredTask(database, expireMilliseconds, deletePriority);
        }
        return this;
    }

    /**
     * 设定过期时间
     *
     * @param database           这个value所在的数据库
     * @param expireMilliseconds 多少毫秒后过期
     *                           null -> 不修改删除计划
     *                           -1 -> 取消删除计划
     *                           -2 -> 立即删除(删除方式: 给数据库提交一个删除任务(低优先级))
     * @param priority           删除优先级
     */
    public HValue<T> clearBy(Database database, Long expireMilliseconds, @Nullable OpsTaskPriority priority) {
        //不修改删除计划
        if (expireMilliseconds == null) {
            return this;
        }
        // 如果设为-1 则为永不过期
        if (expireMilliseconds == -1) {
            cancelClear();
            this.expireMilliseconds = null;
            this.expireDate = null;
            return this;
        }
        if (expireMilliseconds <= -2) {
            submitDelTaskCurrently(database);
            return this;
        }

        this.expireMilliseconds = expireMilliseconds;
        this.expireDate = new Date(System.currentTimeMillis() + expireMilliseconds);
        // 如果不传值, 则不改变原优先级
        if (priority == null) {
            priority = this.deletePriority;
        }
        clearWhenExpiredTask = startExpiredTask(database, expireMilliseconds, priority);
        return this;
    }

    public HValue<T> cancelClear() {
        if (clearWhenExpiredTask == null) {
            return this;
        }
        clearWhenExpiredTask.cancel(false);
        clearWhenExpiredTask = null;
        expireDate = null;
        expireMilliseconds = null;
        return this;
    }

    public boolean isExpired() {
        if (expireDate == null) {
            return false;
        }
        return expireDate.before(new Date());
    }

    /**
     * should call super clone(), but {@link #clone(boolean)} has been already called}
     *
     * @throws CloneNotSupportedException
     */
    @Override
    @SuppressWarnings("all")
    public HValue<T> clone() throws CloneNotSupportedException {
        return clone(false);
    }

    public HValue<T> cloneDefault() {
        return clone(false);
    }

    @SuppressWarnings("unchecked")
    public HValue<T> clone(boolean mutable) {
        try {
            HValue<T> clone = (HValue<T>) super.clone();
            // 默认浅拷贝其它指针域
            clone.clearWhenExpiredTask = null;

//            HValue<T> accessObj = mutable ? clone : ((HValue<T>) ImmutableBean.create(clone));
            Set<String> interceptedMethodNames = Set.of("cancelClear", "clearBy", "clone", "cloneDefault");
            return (HValue<T>) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{HValue.class}, (proxy, method, args) -> {
                if (interceptedMethodNames.contains(method.getName())) {
                    throw new DBSystemException(
                            new UnsupportedOperationException("can`t call method '" + method + "' of" +
                                    " cloned HValue")
                    );
                }

                return method.invoke(clone, args);
            });
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    private static final ByteBuf SET = ByteBufAllocator.DEFAULT.buffer("SET ".getBytes(StandardCharsets.UTF_8).length);

    private static final ByteBuf expireOption = ByteBufAllocator.DEFAULT.buffer("expire=".getBytes(StandardCharsets.UTF_8).length);

    static {
        SET.writeCharSequence("SET", StandardCharsets.UTF_8);
        SET.discardReadBytes();
        expireOption.writeCharSequence("expire=", StandardCharsets.UTF_8);
        expireOption.discardReadBytes();
    }

    public ByteBuf writeCommand(ByteBuf buffer) {
        buffer.writeBytes(SET);
        buffer.writeCharSequence(key, StandardCharsets.UTF_8);
        buffer.writeChar(' ');
        JsonService.transferTo(data, buffer);
        if (expireMilliseconds != null) {
            buffer.writeChar(' ');
            buffer.writeChar('-');
            buffer.writeChar('-');
            if (deletePriority == OpsTaskPriority.LOW) {
                buffer.writeChar('l');
            } else {
                buffer.writeChar('h');
            }
            buffer.writeBytes(expireOption);
            buffer.writeCharSequence(String.valueOf(expireMilliseconds), StandardCharsets.UTF_8);
        }
        return buffer;
    }

    @Override
    public String toString() {
        return JsonService.toString(this);
    }
}
