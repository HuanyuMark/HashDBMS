package org.hashdb.ms.util.reactive;

import org.hashdb.ms.util.AsyncService;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Date: 2024/2/21 19:15
 *
 * @author huanyuMake-pecdle
 */
public class ConcurrentRefReactive<O> extends RefReactive.AbstractRefReactive<O> {
    public ConcurrentRefReactive() {
    }

    public ConcurrentRefReactive(O value) {
        this.value.set(value);
    }

    private final AtomicReference<O> value = new AtomicReference<>();

    private final AtomicReference<ScheduledFuture<?>> debounce = new AtomicReference<>();
    private final Queue<BiConsumer<O, O>> listeners = new ConcurrentLinkedQueue<>();

    @Override
    public O set(O newValue) {
        return value.getAndUpdate(oldValue -> {
            if (oldValue == newValue) {
                setDebounce(null);
                return oldValue;
            }
            if (listeners.isEmpty()) {
                return newValue;
            }
            setDebounce(AsyncService.setTimeout(() -> {
                for (var listener : listeners) {
                    listener.accept(newValue, oldValue);
                }
            }, 100));
            return newValue;
        });
    }

    private void setDebounce(ScheduledFuture<?> newDebounce) {
        debounce.getAndUpdate(oldDebounce -> {
            if (oldDebounce != null) {
                oldDebounce.cancel(true);
            }
            return newDebounce;
        });
    }

    @Override
    public O get() {
        return this.value.get();
    }

    @Override
    public void onChange(BiConsumer<O, O> listener) {
        listeners.add(listener);
    }
}
