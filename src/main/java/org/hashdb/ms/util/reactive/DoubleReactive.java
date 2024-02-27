package org.hashdb.ms.util.reactive;

import org.hashdb.ms.util.AsyncService;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * Date: 2024/2/21 20:20
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class DoubleReactive extends Number implements Reactive {
    private double value;
    protected List<DoubleChangeListener> listeners;

    protected ScheduledFuture<?> debounce;

    public DoubleReactive() {
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
        return ((float) value);
    }

    @Override
    public double doubleValue() {
        return ((double) value);
    }

    public DoubleReactive(double value) {
        this.value = value;
    }

    public double get() {
        return value;
    }

    public double set(double value) {
        if (this.value == value) {
            if (debounce != null) {
                debounce.cancel(true);
            }
            return value;
        }
        double oldValue = this.value;
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

    public interface DoubleChangeListener {
        void onChange(double oldValue, double newValue);
    }

    @Override
    public Class<?> getTargetClass() {
        return double.class;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoubleReactive that)) return false;

        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }
}
