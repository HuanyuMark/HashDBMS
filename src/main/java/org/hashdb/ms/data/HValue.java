package org.hashdb.ms.data;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.config.DBRamConfig;
import org.hashdb.ms.exception.DBInnerException;
import org.hashdb.ms.util.AsyncService;
import org.jetbrains.annotations.NotNull;
import org.springframework.cglib.beans.ImmutableBean;

import java.lang.reflect.Proxy;
import java.util.Arrays;
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
    public final static HValue<?> EMPTY = new HValue<>();

    private final String key;
    private T data;

    private final Date createDate = new Date();

    /**
     * 如果为空，则永不过期
     */
    private Long expireMilliseconds;
    private Date expireDate;
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

    public String key() {
        return key;
    }

    public long ttl() {
        return expireDate == null ? -1 : expireDate.getTime() - System.currentTimeMillis();
    }


    private ScheduledFuture<?> startExpiredTask(Database database, Long expireMilliseconds) {
        cancelClear();
        DBRamConfig dbRamConfig = HashDBMSApp.ctx().getBean(DBRamConfig.class);
        return AsyncService.setTimeout(() -> {
            dbRamConfig.getExpiredKeyClearStrategy().invoke(database, key);
        }, expireMilliseconds);
    }

    /**
     * 设定过期时间
     *
     * @param database           这个value所在的数据库
     * @param expireMilliseconds 多少毫秒后过期
     */
    public HValue<T> clearBy(Database database, Long expireMilliseconds) {
        // TODO: 2023/11/25 null 的话....还没想好是不是要这么处理
        if (expireMilliseconds == null) {
            log.warn("receive null expireMilliseconds: {}", Arrays.stream(Thread.currentThread().getStackTrace()).limit(10).toList());
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
            database.delTask(key).get();
            return this;
        }

        this.expireMilliseconds = expireMilliseconds;
        this.expireDate = new Date(System.currentTimeMillis() + expireMilliseconds);
        if (isExpired()) {
            throw new DBInnerException();
        }
        clearWhenExpiredTask = startExpiredTask(database, expireMilliseconds);
        return this;
    }

//    public HValue<T> clearBy(Database database, Date expireDate) {
//        if (expireMilliseconds == null) {
//            return this;
//        }
//        this.expireDate = expireDate;
//        this.expireMilliseconds = expireDate.getTime() - System.currentTimeMillis();
//        if (isExpired()) {
//            throw new DBInnerException();
//        }
//        clearWhenExpiredTask = startExpiredTask(database, expireMilliseconds);
//        return this;
//    }

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

    @Override
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

            HValue<T> accessObj = mutable ? clone : ((HValue<T>) ImmutableBean.create(clone));
            Set<String> interceptedMethodNames = Set.of("cancelClear", "clearBy", "clone", "cloneDefault");
            return (HValue<T>) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{HValue.class}, (proxy, method, args) -> {
                if (interceptedMethodNames.contains(method.getName())) {
                    throw new DBInnerException(
                            new UnsupportedOperationException("can`t call method '" + method + "' of" +
                                    " cloned HValue")
                    );
                }

                return method.invoke(accessObj, args);
            });
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
