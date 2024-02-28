package org.hashdb.ms.util.reactive;

import org.hashdb.ms.util.AsyncService;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * Date: 2024/2/21 20:20
 *
 * @author Huanyu Mark
 */
public class ByteReactive extends Number implements Reactive {
    private byte value;
    protected List<ByteChangeListener> listeners;

    protected ScheduledFuture<?> debounce;

    public ByteReactive() {
    }

    @Override
    public int intValue() {
        return value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    public ByteReactive(byte value) {
        this.value = value;
    }

    public byte get() {
        return value;
    }

    public byte set(byte value) {
        if (this.value == value) {
            if (debounce != null) {
                debounce.cancel(true);
            }
            return value;
        }
        byte oldValue = this.value;
        this.value = value;
        if (listeners == null) {
            return oldValue;
        }
        if (debounce != null) {
            debounce.cancel(true);
        }
        debounce = AsyncService.setTimeout(() -> {
            for (var listener : listeners) {
                listener.onChange(oldValue, value);
            }
        }, RefReactive.DEBOUNCE_INTERVAL);
        return oldValue;
    }

    @Override
    public Class<?> getTargetClass() {
        return byte.class;
    }

    public interface ByteChangeListener {
        void onChange(byte oldValue, byte newValue);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ByteReactive that)) return false;

        return value == that.value;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
