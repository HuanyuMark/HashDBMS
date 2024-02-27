package org.hashdb.ms.util;

import java.util.function.Supplier;

/**
 * Date: 2023/11/26 23:54
 * 线程安全懒加载. 但是要注意, 如果 {@link #supplier} 的执行耗时过长
 * 会阻塞其它线程获取缓存值
 *
 * @author huanyuMake-pecdle
 */
public class AtomLazy<T> extends Lazy<T> {
    public static <T> AtomLazy<T> of(Supplier<T> supplier) {
        return new AtomLazy<>(supplier);
    }

    private final Object lock = new Object();

    protected AtomLazy(Supplier<T> supplier) {
        super(supplier);
    }

    protected AtomLazy(T initialValue) {
        super(initialValue);
    }

    @Override
    public T get() {
        if (value == null) {
            synchronized (lock) {
                if (value == null) {
                    value = supplier.get();
                }
            }
        }
        return value;
    }
}
