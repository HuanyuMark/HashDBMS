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
public class CharReactive implements Reactive {
    private char value;
    protected List<CharChangeListener> listeners;

    protected ScheduledFuture<?> debounce;

    public CharReactive() {
    }

    public CharReactive(char value) {
        this.value = value;
    }

    public char get() {
        return value;
    }

    public char set(char value) {
        if (this.value == value) {
            if (debounce != null) {
                debounce.cancel(true);
            }
            return value;
        }
        char oldValue = this.value;
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

    public interface CharChangeListener {
        void onChange(char oldValue, char newValue);
    }

    @Override
    public Class<?> getTargetClass() {
        return char.class;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CharReactive that)) return false;

        return value == that.value;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
