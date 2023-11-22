package org.hashdb.ms.data;

import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.config.DBRamConfig;
import org.hashdb.ms.exception.DBInnerException;
import org.hashdb.ms.util.AbortSignal;
import org.hashdb.ms.util.AsyncService;
import org.hashdb.ms.util.Lazy;
import org.intellij.lang.annotations.Identifier;
import org.intellij.lang.annotations.Subst;

import java.util.Date;
import java.util.Objects;

/**
 * Date: 2023/11/21 1:48
 * 描述一个Key的元数据, 类名含义为 HashDBMS hash table 的 key
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
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
    private AbortSignal clearWhenExpiredTask;

    public HKey(@Subst("String") String keyName, Long expireMilliseconds) {
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

    public HKey(@Subst("String") String keyName, Date expireDate) {
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
    public void clearWhenExpiredBy(Database database) {
        if (expireMilliseconds == null) {
            return;
        }
        clearWhenExpiredTask = new AbortSignal();
        DBRamConfig dbRamConfig = HashDBMSApp.ctx().getBean(DBRamConfig.class);
        AsyncService.setTimeout(
                ()-> dbRamConfig.getExpiredKeyClearStrategy().invoke(database,this),
                expireMilliseconds.get(),
                clearWhenExpiredTask
        );
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
    public HKey withExpireDate(Date expireDate) {
        return new HKey(keyName, expireDate);
    }
    public HKey withDelay(long millisecond) {
        // 取消原键的过期清除任务，因为原键与新键的hashcode equal 调用结果一致
        // 即 原键与新键 在 hashtable中语义一致，原键触发清除，会将新键所对应的值也给清除
        cancelClearExpired();
        Date newExpire = new Date(Objects.requireNonNullElse(expireDate.get(), new Date()).getTime() + millisecond);
        return new HKey(keyName, newExpire);
    }
    public void cancelClearExpired() {
        if (clearWhenExpiredTask != null) {
            clearWhenExpiredTask.abort();
        }
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
