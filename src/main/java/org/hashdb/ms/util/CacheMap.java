package org.hashdb.ms.util;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Date: 2024/1/10 23:33
 * 使用 LRU 算法以及 对数增长时间淘汰算法 淘汰值
 *
 * @author Huanyu Mark
 */
public class CacheMap<K, V> {

    private final LinkedHashMap<K, CacheValue> base;
    /**
     * 所有键被命中后, 都会在这个基础上增加存活时间, 以一个ln的速率增加
     */
    @Getter
    private long aliveTime;

    @Getter
    private final int cacheSize;

    private Function<K, V> accessor;

    private BiFunction<K, V, V> saver;

    public CacheMap() {
        this(-1, Integer.MAX_VALUE, 16, 0.75F);
    }

    public CacheMap(long aliveTime, int cacheSize) {
        this(aliveTime, cacheSize, 16, 0.75F);
    }

    public CacheMap(long aliveTime, int cacheSize, int initialCapacity) {
        this(aliveTime, cacheSize, initialCapacity, 0.75F);
    }

    public CacheMap(long aliveTime, int cacheSize, int initialCapacity, float loadFactor) {
        // LRU  根据最近访问次数排优先队列
        base = new LinkedHashMap<>(initialCapacity, loadFactor, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CacheValue> eldest) {
                if (size() < cacheSize) {
                    return false;
                }
                eldest.getValue().cancel();
                return true;
            }
        };
        setAliveTime(aliveTime);
        this.cacheSize = cacheSize;
    }

    public void setAliveTime(long aliveTime) {
        this.aliveTime = aliveTime;
        if (aliveTime > 0) {
            accessor = key -> {
                CacheValue res = base.get(key);
                if (res == null) {
                    return null;
                }
                res.hitAndDelay();
                return res.value;
            };
            saver = (key, value) -> {
                if (value == null) {
                    throw new NullPointerException();
                }
                var v = new CacheValue(key, value);
                v.hitAndDelay();
                var oldValue = base.put(key, v);
                if (oldValue == null) {
                    return null;
                }
                oldValue.cancel();
                return oldValue.value;
            };
            return;
        }
        accessor = key -> {
            CacheValue res = base.get(key);
            if (res == null) {
                return null;
            }
            return res.value;
        };
        saver = (key, value) -> {
            if (value == null) {
                throw new NullPointerException();
            }
            var oldValue = base.put(key, new CacheValue(key, value));
            if (oldValue == null) {
                return null;
            }
            return oldValue.value;
        };
    }

    public V save(@NotNull K key, V value) {
        return saver.apply(key, value);
    }

    public V hit(K key) {
        return accessor.apply(key);
    }

    public V expire(@NotNull K key) {
        CacheValue res = base.remove(key);
        if (res == null) {
            return null;
        }
        res.cancel();
        return res.value;
    }

    public V get(@NotNull K key) {
        return hit(key);
    }

    public V put(@NotNull K key, V value) {
        return save(key, value);
    }

    public V remove(@NotNull K key) {
        return expire(key);
    }

    private class CacheValue {
        private final K key;
        protected final V value;

        /**
         * 命中次数
         */
        private final AtomicLong hitCount = aliveTime <= 0 ? null : new AtomicLong(0);

        private volatile boolean canceled = false;

        CacheValue(K key, V value) {
            this.key = key;
            this.value = value;
        }

        void cancel() {
            canceled = true;
        }

        /**
         * 线程安全, 在本线程只执行了一次涉及共享变量的原子操作
         */
        void hitAndDelay() {
            long hitVersion = hitCount.getAndIncrement();
            long newAliveTime = (long) (aliveTime * Math.log(Math.E + hitVersion));
            AsyncService.setTimeout(() -> {
                if (canceled || hitVersion < hitCount.get()) {
                    return;
                }
                expire(key);
            }, newAliveTime);
        }
    }
}
