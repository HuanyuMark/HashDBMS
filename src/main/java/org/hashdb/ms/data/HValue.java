package org.hashdb.ms.data;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.config.DBRamConfig;
import org.hashdb.ms.exception.DBInnerException;
import org.hashdb.ms.exception.ServiceStoppedException;
import org.hashdb.ms.util.AsyncService;
import org.hashdb.ms.util.JacksonSerializer;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.cglib.beans.ImmutableBean;

import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

/**
 * Date: 2023/11/21 2:37
 * 在{@link Database} 中存储 K - V 的pair
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public class HValue<T> implements Cloneable {
    public final static Lazy<DBRamConfig> dbRamConfig = Lazy.of(() -> HashDBMSApp.ctx().getBean(DBRamConfig.class));
    public final static HValue<?> EMPTY = new HValue<>();

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
    private final Date createDate = new Date();

    /**
     * 如果为空，则永不过期
     */
    @Getter
    private Long expireMilliseconds;
    @Getter
    private Date expireDate;

    /**
     * 如果为高,则触发删除任务时, 将删除任务插入任务队列头部, 优先执行
     */
    @Getter
    private OpsTaskPriority deletePriority = dbRamConfig.get().getExpiredKeyDeletePriority();
    /**
     * 是否取消， 当该key过期时， 清除该key
     */
    private ScheduledFuture<?> clearWhenExpiredTask;

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

    public Object data() {
        return data;
    }


    public DataType dataType() {
        return DataType.typeofHValue(this);
    }

    public void data(T data) {
        Objects.requireNonNull(data);
        this.data = data;
    }

    public void setData(T data) {
        Objects.requireNonNull(data);
        this.data = data;
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
        deletePriority = Objects.requireNonNullElse(priority, DBRamConfig.DEFAULT_EXPIRED_KEY_DELETE_PRIORITY.get());
        return AsyncService.setTimeout(() -> {
            var deleteTask = OpsTask.of(() -> database.del(key));
            if (deletePriority == OpsTaskPriority.HIGH) {
                database.submitOpsTask(deleteTask, OpsTaskPriority.HIGH);
            } else {
                database.submitOpsTask(deleteTask);
            }
        }, expireMilliseconds);
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
            var deleteTask = OpsTask.of(() -> database.del(key));
            if (deletePriority == OpsTaskPriority.HIGH) {
                database.submitOpsTask(deleteTask, OpsTaskPriority.HIGH);
            } else {
                database.submitOpsTask(deleteTask);
            }
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

    @Deprecated
    public HValue<T> clearBy(Database database, Date expireDate, @Nullable OpsTaskPriority priority) {
        if (expireMilliseconds == null) {
            return this;
        }
        this.expireDate = expireDate;
        this.expireMilliseconds = expireDate.getTime() - System.currentTimeMillis();
        if (isExpired()) {
            throw new DBInnerException();
        }
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
                    throw new DBInnerException(
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HValue<?> hValue)) return false;

        if (!Objects.equals(key, hValue.key)) return false;
        if (!Objects.equals(data, hValue.data)) return false;
        if (!createDate.equals(hValue.createDate)) return false;
        if (!Objects.equals(expireMilliseconds, hValue.expireMilliseconds))
            return false;
        if (!Objects.equals(expireDate, hValue.expireDate)) return false;
        return deletePriority == hValue.deletePriority;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (data != null ? data.hashCode() : 0);
        result = 31 * result + createDate.hashCode();
        result = 31 * result + (expireMilliseconds != null ? expireMilliseconds.hashCode() : 0);
        result = 31 * result + (expireDate != null ? expireDate.hashCode() : 0);
        result = 31 * result + (deletePriority != null ? deletePriority.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return JacksonSerializer.stringfy(this);
    }
}
