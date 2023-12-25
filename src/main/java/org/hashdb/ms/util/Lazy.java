package org.hashdb.ms.util;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Date: 2023/11/13 19:38
 * 延迟加载类, 第一次使用 {@link #get()} 时, 会调用 {@link #supplier} 获取初值并缓存
 * 后面调用 {@link #get()} 时, 直接返回缓存下来的值
 * 如果 supplier 运行后没有产生有效值 比如:{@code null}, 则下次调用 {@link #get()}
 * 依旧会尝试运行 supplier 来产生值, 进行缓存
 * 如果只需要运行supplier一次, 可以使用其子类 {@link OneTimeLazy}
 */
public class Lazy<T> {
    protected volatile T value;

    protected final Supplier<T> supplier;

    protected Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    protected Lazy(T initValue) {
        value = initValue;
        this.supplier = () -> initValue;
    }

    protected Lazy() {
        supplier = () -> null;
    }

    public static <T> Lazy<T> of(Supplier<T> supplier) {
        Objects.requireNonNull(supplier);
        return new Lazy<>(supplier);
    }

    public static <T> Lazy<T> of(T value) {
        Objects.requireNonNull(value);
        return new Lazy<>(value);
    }

    public static <T> Lazy<T> empty() {
        return new Lazy<>();
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

    public boolean isCached() {
        return value != null;
    }

    /**
     * @param value 后期给定的值， 直接跳过 {@link #supplier} 的调用， 直接使用给定值
     */
    public void computedWith(T value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lazy<?> lazy)) return false;

        return Objects.equals(value, lazy.value);
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}
