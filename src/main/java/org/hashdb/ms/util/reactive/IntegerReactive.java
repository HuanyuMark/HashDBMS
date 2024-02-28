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
public class IntegerReactive extends Number implements Reactive {
    private int value;
    protected List<IntegerChangeListener> listeners;

    protected ScheduledFuture<?> debounce;

    public IntegerReactive() {
    }

    public IntegerReactive(int value) {
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

    public int get() {
        return value;
    }

    public int set(int value) {
        if (this.value == value) {
            if (debounce != null) {
                debounce.cancel(true);
            }
            return value;
        }
        int oldValue = this.value;
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

    public interface IntegerChangeListener {
        void onChange(int oldValue, int newValue);
    }

    @Override
    public Class<?> getTargetClass() {
        return int.class;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IntegerReactive that)) return false;

        return value == that.value;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
