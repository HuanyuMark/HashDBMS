package org.hashdb.ms.util.reactive;

import org.hashdb.ms.util.AsyncService;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * Date: 2024/2/21 20:19
 *
 * @author Huanyu Mark
 * @version 0.0.1
 */
public class ShortReactive extends Number implements Reactive {
    private short value;
    protected List<ShortChangeListener> listeners;

    protected ScheduledFuture<?> debounce;

    public ShortReactive() {
    }

    public ShortReactive(short value) {
        this.value = value;
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

    public short get() {
        return value;
    }

    public short set(short value) {
        if (this.value == value) {
            if (debounce != null) {
                debounce.cancel(true);
            }
            return value;
        }
        short oldValue = this.value;
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

    public interface ShortChangeListener {
        void onChange(int oldValue, int newValue);
    }

    @Override
    public Class<?> getTargetClass() {
        return short.class;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShortReactive that)) return false;

        return value == that.value;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
