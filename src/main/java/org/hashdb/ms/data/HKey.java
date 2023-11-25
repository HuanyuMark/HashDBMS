package org.hashdb.ms.data;

import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.config.DBRamConfig;
import org.hashdb.ms.exception.DBInnerException;
import org.hashdb.ms.util.AbortSignal;
import org.hashdb.ms.util.AsyncService;
import org.hashdb.ms.util.Lazy;
import org.intellij.lang.annotations.Identifier;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

/**
 * Date: 2023/11/21 1:48
 * 描述一个Key的元数据, 类名含义为 HashDBMS hash table 的 key
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Deprecated
public class HKey {
    @Identifier
    private final String keyName;
    private final Date createDate = new Date();
    /**
     * 如果为空，则永不过期
     */
    private final Lazy<Long> expireMilliseconds;
    private final Lazy<Date> expireDate;
    /**
     * 是否取消， 当该key过期时， 清除该key
     */
    private ScheduledFuture<?> clearWhenExpiredTask;

    public HKey(@Subst("String") String keyName,@Nullable Long expireMilliseconds) {
        this.keyName = keyName;
        if(expireMilliseconds == null) {
            this.expireMilliseconds = Lazy.of(null);
            this.expireDate = Lazy.of(null);
            return;
        }
        this.expireMilliseconds = Lazy.of(expireMilliseconds);
        this.expireDate = Lazy.of(()-> new Date(createDate.getTime()+expireMilliseconds));
        if(isExpired()) {
            throw new DBInnerException("expire date '"+expireDate+"' is expired");
        }
    }

    public HKey(@Subst("String") String keyName,@Nullable Date expireDate) {
        this.keyName = keyName;
        if(expireDate == null) {
            this.expireMilliseconds = Lazy.of(null);
            this.expireDate = Lazy.of(null);
            return;
        }
        this.expireDate = Lazy.of(expireDate);
        this.expireMilliseconds = Lazy.of(() -> expireDate.getTime() - createDate.getTime());
        if(isExpired()) {
            throw new DBInnerException("expire date '"+expireDate+"' is expired");
        }
    }
    public HKey clearBy(Database database) {
        if (expireMilliseconds == null) {
            return this;
        }
        DBRamConfig dbRamConfig = HashDBMSApp.ctx().getBean(DBRamConfig.class);
        clearWhenExpiredTask = AsyncService.setTimeout(
                ()-> dbRamConfig.getExpiredKeyClearStrategy().invoke(database,keyName),
                expireMilliseconds.get()
        );
        return this;
    }

    public String getName() {
        return keyName;
    }

    public Date getExpireDate() {
        return new Date();
    }

    public Date getCreateDate() {
        return createDate;
    }
    public boolean isExpired() {
        return expireDate.get() != null && expireDate.get().before(new Date());
    }

    /**
     * 记得在延迟后，手动调用 {@link #clearBy(Database db)}
     * @param expireDate 在此时间后过期
     */
    public HKey withExpireDate(Date expireDate) {
        if(expireDate == null) {
            return new HKey(keyName, (Date) null);
        }
        if(expireDate.after(new Date())) {
            throw new DBInnerException();
        }
        cancelClearExpired();
        return new HKey(keyName, expireDate);
    }

    /**
     * 记得在延迟后，手动调用 {@link #clearBy(Database db)}
     * @param millisecond 多少毫秒后删除该键
     */
    public HKey withDelay(Long millisecond) {
        // 取消原键的过期清除任务，因为原键与新键的hashcode equal 调用结果一致
        // 即 原键与新键 在 hashtable中语义一致，原键触发清除，会将新键所对应的值也给清除
        Date newExpire = Optional.ofNullable(millisecond)
                .map(t-> new Date(Objects.requireNonNullElse(expireDate.get(), new Date()).getTime() + millisecond))
                .orElse(null);
        return withExpireDate(newExpire);
    }
    public HKey cancelClearExpired() {
        if (clearWhenExpiredTask != null) {
            clearWhenExpiredTask.cancel(false);
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        // 只以keyName为主字段
        if (o instanceof String str) {
            return Objects.equals(str, keyName);
        }

        if (!(o instanceof HKey that)) return false;
        return Objects.equals(keyName, that.keyName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(keyName);
    }
}
