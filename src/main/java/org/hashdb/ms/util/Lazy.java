package org.hashdb.ms.util;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Date: 2023/11/13 19:38
 * 延迟加载类, 第一次使用 {@link #get()} 时, 会调用 {@link #supplier} 获取初值并缓存
 * 后面调用 {@link #get()} 时, 直接返回缓存下来的值
 */
public class Lazy<T> {
    private volatile T value;
    private final Supplier<T> supplier;
    protected Lazy(Supplier<T> supplier) {
        Objects.requireNonNull(supplier);
        this.supplier = supplier;
    }
    public static <T> Lazy<T> of(Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }
    public static <T> Lazy<T> of(T value) {
        return new Lazy<>(()->value);
    }
    /**
     * 线程不安全, {@link #supplier} 可能会被多次调用
     */
    public T get() {
        if (value != null) {
            return value;
        }
        value = supplier.get();
        return value;
    }

    /**
     * @param value 后期给定的值， 直接跳过 {@link #supplier} 的调用， 直接使用给定值
     */
    public void computedWith(T value) {
        this.value = value;
    }
}
