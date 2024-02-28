package org.hashdb.ms.util;

import org.jetbrains.annotations.NotNull;

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
public class Lazy<T> implements Supplier<T> {
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

    public static <T> AtomLazy<T> ofAtomic(Supplier<T> supplier) {
        Objects.requireNonNull(supplier);
        return new AtomLazy<>(supplier);
    }

    public static <T> AtomLazy<T> ofAtomic(T value) {
        Objects.requireNonNull(value);
        return new AtomLazy<>(value);
    }

    public static <T> Lazy<T> empty() {
        return new Lazy<>();
    }

    /**
     * 线程不安全, {@link #supplier} 可能会被多次调用,
     * 可以选用 {@link AtomLazy<T>} 来确保线程安全
     */
    @Override
    public T get() {
        T res = value;
        if (res == null) {
            res = supplier.get();
            value = res;
            return res;
        }
        return res;
    }

    public @NotNull T getOrThrow(Supplier<Exception> e) throws Exception {
        T res = get();
        if (res == null) {
            throw e.get();
        }
        return res;
    }

    public boolean isResolved() {
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
        return Objects.hashCode(value);
    }

    public boolean supplierEquals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lazy<?> lazy)) return false;

        return Objects.equals(supplier, lazy.supplier);
    }

    public int supplierHashCode() {
        return Objects.hashCode(supplier);
    }

    @Override
    public String toString() {
        return value == null ? "[Lazy: Unresolved]" : STR."[Lazy: \{value}]";
    }
}
