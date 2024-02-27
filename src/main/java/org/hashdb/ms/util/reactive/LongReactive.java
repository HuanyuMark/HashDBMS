package org.hashdb.ms.util.reactive;

import org.hashdb.ms.util.AsyncService;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * Date: 2024/2/21 20:19
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class LongReactive extends Number implements Reactive {
    private long value;
    protected List<LongChangeListener> listeners;

    protected ScheduledFuture<?> debounce;

    public LongReactive() {
    }

    public LongReactive(int value) {
        this.value = value;
    }

    @Override
    public int intValue() {
        return (int) value;
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

    public long get() {
        return value;
    }

    public long set(long value) {
        if (this.value == value) {
            if (debounce != null) {
                debounce.cancel(true);
            }
            return value;
        }
        long oldValue = this.value;
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

    public interface LongChangeListener {
        void onChange(long oldValue, long newValue);
    }

    @Override
    public Class<?> getTargetClass() {
        return long.class;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LongReactive that)) return false;

        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }
}
