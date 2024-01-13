package org.hashdb.ms.util;

import java.util.concurrent.ScheduledFuture;

/**
 * Date: 2024/1/13 21:08
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class CacheValue<K, Raw> {
    protected Raw value;
    private final K key;

    private final CacheMap<K, Raw> container;

    /**
     * 命中次数
     */
    int hitCount = 0;
    /**
     * 毫秒时间戳
     * -1 永久保存
     * 其他值, 到这个时间戳删除
     */
//        private long expireTimestamp = -1;

    private ScheduledFuture<?> expireExecutor;


    public CacheValue(K key, Raw value, CacheMap<K, Raw> container) {
        this.key = key;
        this.value = value;
        this.container = container;
    }

    void delay(long ms) {
        if (ms <= -1 || ms < System.currentTimeMillis()) {
            container.remove(key);
            return;
        }
        expireExecutor = AsyncService.setTimeout(() -> container.remove(key), ms);
    }

    public Raw get() {
        return value;
    }

    public void set(Raw value) {
        if (value == null) {
            container.remove(key);
        }
        this.value = value;
    }

    void cancel() {
        if (expireExecutor != null) {
            expireExecutor.cancel(true);
            expireExecutor = null;
        }
    }
}
