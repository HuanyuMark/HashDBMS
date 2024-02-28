package org.hashdb.ms.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Date: 2024/2/17 14:49
 *
 * @author Huanyu Mark
 */
public class LongIdentityGenerator {
    private final long start;

    private final long end;

    private final AtomicLong OLD_ID;

    /**
     * @param start 不包括start
     * @param end   包括end
     */
    public LongIdentityGenerator(long start, long end) {
        this.start = start;
        this.end = end;
        OLD_ID = new AtomicLong(start);
    }

    public long incrementId() {
        return nextId(1);
    }

    public long decrementId() {
        return nextId(-1);
    }

    public long nextId() {
        return incrementId();
    }

    public long nextId(long delta) {
        while (true) {
            var oldValue = OLD_ID.get();
            if (oldValue < end) {
                // 当前值未达到阈值，直接递增
                long newValue = oldValue + delta;
                if (OLD_ID.compareAndSet(oldValue, newValue)) {
                    return newValue;
                }
                continue;
            }
            OLD_ID.compareAndSet(end, start);
        }
    }

    /**
     * 请注意, 该方法与其它语句同时使用, 不能确保线程安全, 需要在调用方注意
     */
    public void reset() {
        OLD_ID.set(start);
    }
}
