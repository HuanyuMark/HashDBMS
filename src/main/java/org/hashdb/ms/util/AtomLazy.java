package org.hashdb.ms.util;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Date: 2023/11/26 23:54
 * 线程安全懒加载. 但是要注意, 如果 {@link #supplier} 的执行耗时过长
 * 可能会阻塞其它线程获取缓存值
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AtomLazy<?> atomLazy)) {
            if (o instanceof Lazy<?> lazy) {
                return Objects.equals(value, lazy.value);
            }
            return false;
        }
        if (!super.equals(o)) return false;

        return value.equals(atomLazy.value);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}
