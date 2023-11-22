package org.hashdb.ms.data;

import org.hashdb.ms.util.Futures;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Date: 2023/11/22 11:40
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface DequeTask<T> extends Supplier<T> {
    T result();
    @Contract(value = "_ -> new", pure = true)
    static <T> @NotNull DequeTask<T> of(Supplier<T> supplier) {
        return new DequeTask<>() {
            private final CompletableFuture<T> result = new CompletableFuture<>();
            @Override
            public T result() {
                return Futures.unwrap(result);
            }
            @Override
            public T get() {
                T res = supplier.get();
                result.complete(res);
                return res;
            }
        };
    }

    static DequeTask<Void> of(Runnable task){
        return new DequeTask<>() {
            private final CompletableFuture<Void> result = new CompletableFuture<>();
            @Override
            public Void result() {
                return Futures.unwrap(result);
            }
            @Override
            public Void get() {
                task.run();
                result.complete(null);
                return null;
            }
        };
    }
}
