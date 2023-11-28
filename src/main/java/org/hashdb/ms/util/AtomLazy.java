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

    protected AtomLazy(T initialValue) {
        value.set(initialValue);
    }
    @Override
    public T get() {
        value.compareAndSet(null, supplier.get());
        return value.get();
    }

    @Override
    public boolean isCached() {
        return value.get() != null;
    }

    @Override
    public void computedWith(T value) {
        this.value.compareAndSet(null,value);
    }

    public void replaceWith(T value) {
        this.value.set(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AtomLazy<?> atomLazy)) return false;
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
