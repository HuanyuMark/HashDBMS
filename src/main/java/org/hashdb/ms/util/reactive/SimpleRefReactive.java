package org.hashdb.ms.util.reactive;

import org.hashdb.ms.util.AsyncService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BiConsumer;

/**
 * Date: 2024/2/21 20:21
 *
 * @author Huanyu Mark
 */
public class SimpleRefReactive<O> extends RefReactive.AbstractRefReactive<O> {

    protected O value;

    protected List<BiConsumer<O, O>> listeners;

    protected ScheduledFuture<?> debounce;

    public SimpleRefReactive() {
    }

    public SimpleRefReactive(O value) {
        this.value = value;
    }

    @Override
    public O set(O value) {
        if (this.value == value) {
            if (debounce != null) {
                debounce.cancel(true);
            }
            return value;
        }
        O oldValue = this.value;
        this.value = value;
        if (listeners == null) {
            return oldValue;
        }
        if (debounce != null) {
            debounce.cancel(true);
        }
        debounce = AsyncService.setTimeout(() -> {
            for (var listener : listeners) {
                listener.accept(oldValue, value);
            }
        }, RefReactive.DEBOUNCE_INTERVAL);
        return oldValue;
    }

    @Override
    public O get() {
        return value;
    }

    @Override
    public void onChange(BiConsumer<O, O> listener) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);
    }
}
