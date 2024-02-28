package org.hashdb.ms.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: 2024/2/17 14:58
 *
 * @author Huanyu Mark
 */
public class IntegerIdentityGenerator {
    private final int start;

    private final int end;

    private final AtomicInteger OLD_ID;

    /**
     * @param start 不包括start
     * @param end   包括end
     */
    public IntegerIdentityGenerator(int start, int end) {
        this.start = start;
        this.end = end;
        OLD_ID = new AtomicInteger(start);
    }

    public int incrementId() {
        return nextId(1);
    }

    public int decrementId() {
        return nextId(-1);
    }

    public int nextId() {
        return incrementId();
    }

    public int nextId(int delta) {
        while (true) {
            var oldValue = OLD_ID.get();
            if (oldValue < end) {
                // 当前值未达到阈值，直接递增
                int newValue = oldValue + delta;
                if (OLD_ID.compareAndSet(oldValue, newValue)) {
                    return newValue;
                }
                continue;
            }
            // 当前值达到或超过阈值，尝试重置为0, 无论是否设置成功,最后一定有一个线程成功设置为0,只需要在下次递增时获取新id即可
            OLD_ID.compareAndSet(end, start);
        }
    }
}
