package org.hashdb.ms.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public interface OpsTask<T> extends Supplier<T> {
    T result();

    CompletableFuture<T> future();
    @Slf4j
    @RequiredArgsConstructor
    class OpsTaskImpl<T> implements OpsTask<T> {
        public static final OpsTask<?> EMPTY = new OpsTaskImpl<>(()-> null);
        private final CompletableFuture<T> future = new CompletableFuture<>();
        private final Supplier<T> supplier;
        @Override
        public T result() {
            return Futures.unwrap(future);
        }
        @Override
        public CompletableFuture<T> future(){return future;}
        @Override
        public T get() {
            try {
                T res = supplier.get();
                future.complete(res);
                return res;
            } catch (Throwable e) {
                log.info("supplier throw exception: {}",e.toString());
                future.completeExceptionally(e);
            }
            return null;
        }
    }
    @Contract(value = "_ -> new", pure = true)
    static <T> @NotNull OpsTask<T> of(Supplier<T> supplier) {
        return new OpsTaskImpl<>(supplier);
    }

    @Contract("_ -> new")
    static @NotNull OpsTask<?> of(Runnable task){
        return new OpsTaskImpl<>(()-> {
            task.run();
            return null;
        });
    }
    static @NotNull OpsTask<?> empty(){
        return OpsTaskImpl.EMPTY;
    }
}
