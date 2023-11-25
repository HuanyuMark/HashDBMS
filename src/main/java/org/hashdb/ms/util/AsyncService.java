package org.hashdb.ms.util;

import java.util.concurrent.*;
import java.util.function.Supplier;
/**
 * Date: 2023/11/14 13:37
 * 异步服务支持, 提供一个全局的公用线程池
 */
public class AsyncService {
    private static final Lazy<ExecutorService> executorService = Lazy.of(()->{
        ThreadFactory threadFactory = Thread.ofVirtual()
                .name("v-t-executor-", 0)
                .inheritInheritableThreadLocals(true)
                .uncaughtExceptionHandler((thread, e) -> {
                    System.err.printf("Thread: [%s] throw exception: %s\n", thread.getName(), e);
                })
                .factory();
        return Executors.newThreadPerTaskExecutor(threadFactory);
    });

    private static final Lazy<ScheduledExecutorService> scheduledExecutorService = Lazy.of(()->{
        ThreadFactory threadFactory = Thread.ofVirtual()
               .name("v-s-executor-", 0)
               .inheritInheritableThreadLocals(true)
               .uncaughtExceptionHandler((thread, e) -> {
                    System.err.printf("Thread: [%s] throw exception: %s\n", thread.getName(), e);
                })
               .factory();
        return Executors.newScheduledThreadPool(0,threadFactory);
    });
    public static ExecutorService service(){
        return executorService.get();
    }
    public static <T> CompletableFuture<T> submit(Supplier<T> supplier){
        return CompletableFuture.supplyAsync(supplier, service());
    }
    public static CompletableFuture<Void> submit(Runnable task){
        return CompletableFuture.runAsync(task, service());
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
