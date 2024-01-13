package org.hashdb.ms.util;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Date: 2024/1/10 23:33
 * 使用 LRU 算法以及 对数增长时间淘汰算法 淘汰值
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class CacheMap<K, V> extends LinkedHashMap<K, CacheValue<K, V>> {

    /**
     * 当这个map的元素都被清空时,在{@link  #aliveTime} 毫秒后,通知
     * 监听者, 这个map已经闲置了 {@link #aliveTime} 毫秒
     */
    @Setter
    @Getter
    private long aliveTime;

    @Setter
    @Getter
    private int cacheSize;

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
        super(initialCapacity, loadFactor, true);
        this.aliveTime = aliveTime;
        this.cacheSize = cacheSize;
    }

    public CacheValue<K, V> putRaw(@NotNull K key, V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        CacheValue<K, V> v = new CacheValue<>(key, value, this);
        v.delay(aliveTime);
        return super.put(key, v);
    }

    @Override
    public CacheValue<K, V> get(Object key) {
        CacheValue<K, V> res = super.get(key);
        if (res != null) {
            res.delay((long) (aliveTime * Math.log(Math.E + ++res.hitCount)));
        }
        return res;
    }

    @Override
    public CacheValue<K, V> remove(@NotNull Object key) {
        CacheValue<K, V> res = super.remove(key);
        if (res != null) {
            res.cancel();
        }
        return res;
    }

    @Override
    public boolean remove(Object key, Object value) {
        boolean ok = super.remove(key, value);
        if (ok && value instanceof CacheValue<?, ?> v) {
            v.cancel();
        }
        return ok;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, CacheValue<K, V>> eldest) {
        if (size() >= cacheSize) {
            eldest.getValue().cancel();
            return true;
        }
        return false;
    }
}
