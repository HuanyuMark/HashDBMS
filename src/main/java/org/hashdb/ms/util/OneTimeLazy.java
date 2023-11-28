package org.hashdb.ms.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Date: 2023/11/26 23:45
 * 与 Lazy 不同的是, 这个类的 supplier 只会运行一次, 无论 supplier 是否有返回值
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class OneTimeLazy<T> extends Lazy<T> {
    public static <T> OneTimeLazy<T> of(Supplier<T> supplier) {
        return new OneTimeLazy<>(supplier);
    }

    private final AtomicBoolean ran = new AtomicBoolean(false);

    protected OneTimeLazy(Supplier<T> supplier) {
        super(supplier);
    }

    @Override
    public T get() {
        if (ran.compareAndSet(false, true)) {
            value = supplier.get();
            return value;
        }
        return value;
    }
}
