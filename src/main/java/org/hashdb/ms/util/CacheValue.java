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
    private int hitCount = 0;

    private ScheduledFuture<?> expireExecutor;

    public CacheValue(K key, Raw value, CacheMap<K, Raw> container) {
        this.key = key;
        this.value = value;
        this.container = container;
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

    synchronized void hitAndDelay(long ms) {
        expireExecutor = AsyncService.setTimeout(() -> container.remove(key), (long) (ms * Math.log(Math.E + hitCount++)));
    }
}
