package org.hashdb.ms.util.reactive;

import org.hashdb.ms.util.AsyncService;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * Date: 2024/2/21 20:20
 *
 * @author Huanyu Mark
 * @version 0.0.1
 */
public class FloatReactive extends Number implements Reactive {
    private float value;
    protected List<FloatChangeListener> listeners;

    protected ScheduledFuture<?> debounce;

    public FloatReactive() {
    }


    public FloatReactive(float value) {
        this.value = value;
    }

    @Override
    public int intValue() {
        return ((int) value);
    }

    @Override
    public long longValue() {
        return ((long) value);
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }


    public float get() {
        return value;
    }

    public float set(float value) {
        if (this.value == value) {
            if (debounce != null) {
                debounce.cancel(true);
            }
            return value;
        }
        float oldValue = this.value;
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

    public interface FloatChangeListener {
        void onChange(float oldValue, float newValue);
    }

    @Override
    public Class<?> getTargetClass() {
        return float.class;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FloatReactive that)) return false;

        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(value);
    }
}
