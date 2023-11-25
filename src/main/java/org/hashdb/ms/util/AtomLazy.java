package org.hashdb.ms.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Date: 2023/11/26 23:54
 * 线程安全懒加载. 但是要注意, 如果 {@link #supplier} 的执行耗时过长
 * 可能会阻塞其它线程获取缓存值
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class AtomLazy<T> extends Lazy<T> {
    public static <T> AtomLazy<T> of(Supplier<T> supplier) {
        return new AtomLazy<>(supplier);
    }

    protected final AtomicReference<T> value = new AtomicReference<>();

    protected AtomLazy(Supplier<T> supplier) {
        super(supplier);
    }

    @Override
    public T get() {
        value.compareAndSet(null, supplier.get());
        return value.get();
    }
}
