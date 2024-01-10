package org.hashdb.ms.util;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Date: 2023/11/14 13:37
 * 提供一个全局的异步服务支持
 */
public class AsyncService {
    private static final Lazy<ExecutorService> executorService = AtomLazy.of(() -> {
        ThreadFactory threadFactory = Thread.ofVirtual()
                .name("vt-", 0)
                .inheritInheritableThreadLocals(true)
                .uncaughtExceptionHandler((thread, e) -> {
                    System.err.printf("Thread: [%s] throw exception: %s\n", thread.getName(), e);
                })
                .factory();
        return Executors.newThreadPerTaskExecutor(threadFactory);
    });

    private static final Lazy<ScheduledExecutorService> scheduledExecutorService = AtomLazy.of(() -> {
        ThreadFactory threadFactory = Thread.ofVirtual()
                .name("vs-", 0)
                .inheritInheritableThreadLocals(true)
                .uncaughtExceptionHandler((thread, e) -> {
                    System.err.printf("Thread: [%s] throw exception: %s\n", thread.getName(), e);
                })
                .factory();
        return Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() >> 1, threadFactory);
    });

    public static ExecutorService service() {
        return executorService.get();
    }

    public static <T> CompletableFuture<T> start(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, service());
    }

    public static List<? extends CompletableFuture<?>> start(Supplier<?>... supplier) {
        return Arrays.stream(supplier).map(s -> CompletableFuture.supplyAsync(s, service())).toList();
    }

    public static CompletableFuture<?> start(Runnable task) {
        return CompletableFuture.runAsync(task, service());
    }

    public static List<? extends CompletableFuture<?>> start(Runnable... task) {
        return Arrays.stream(task).map(AsyncService::start).toList();
    }

    public static ScheduledFuture<?> setTimeout(Runnable runnable, long milliseconds) {
        return scheduledExecutorService.get().schedule(runnable, milliseconds, TimeUnit.MILLISECONDS);
    }

    public static <T> ScheduledFuture<T> setTimeout(Callable<T> callable, long milliseconds) {
        return scheduledExecutorService.get().schedule(callable, milliseconds, TimeUnit.MILLISECONDS);
    }

    public static ScheduledFuture<?> setInterval(Runnable runnable, long milliseconds) {
        return scheduledExecutorService.get().scheduleAtFixedRate(runnable, 0, milliseconds, TimeUnit.MILLISECONDS);
    }

    public static ScheduledFuture<?> setInterval(Runnable runnable, long milliseconds, long initialDelayMilliseconds) {
        return scheduledExecutorService.get().scheduleAtFixedRate(runnable, initialDelayMilliseconds, milliseconds, TimeUnit.MILLISECONDS);
    }
}
