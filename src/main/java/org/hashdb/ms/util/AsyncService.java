package org.hashdb.ms.util;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Date: 2023/11/14 13:37
 * 使用虚拟线程提供一个全局的异步服务支持. 并发量等配置, 需要使用设置
 * {@link ForkJoinPool#commonPool()} 的配置
 */
public class AsyncService {
    private static final ExecutorService simpleExecutor;

    private static final ScheduledExecutorService perTaskScheduledExecutor;

    static {
        var executorServiceThreadFactory = Thread.ofVirtual()
                .name("vt-", 0)
                .inheritInheritableThreadLocals(true)
                .uncaughtExceptionHandler((thread, e) -> {
                    System.err.printf("Thread: [%s] throw exception: %s\n", thread.getName(), e);
                })
                .factory();
        simpleExecutor = Executors.newThreadPerTaskExecutor(executorServiceThreadFactory);
        var sheduleThreadFactory = Thread.ofVirtual()
                .name("vs-", 0)
                .inheritInheritableThreadLocals(true)
                .uncaughtExceptionHandler((thread, e) -> {
                    System.err.printf("Thread: [%s] throw exception: %s\n", thread.getName(), e);
                })
                .factory();
        perTaskScheduledExecutor = new ScheduledThreadPoolExecutor(0, sheduleThreadFactory);
    }

    public static ThreadFactory virtualFactory(String threadPrefix) {
        return Thread.ofVirtual()
                .name(threadPrefix, 0)
                .inheritInheritableThreadLocals(true)
                .uncaughtExceptionHandler((thread, e) -> {
                    System.err.printf("Thread: [%s] throw exception: %s\n", thread.getName(), e);
                })
                .factory();
    }

    public static ExecutorService service() {
        return simpleExecutor;
    }

    /**
     * @return 如果在时限内正常关闭, 则true否则false
     */
    public static boolean close(int time, TimeUnit timeUnit) {
        simpleExecutor.shutdown();
        try {
            return simpleExecutor.awaitTermination(time, timeUnit);
        } catch (InterruptedException e) {
            return false;
        }
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

    public static void run(Runnable task) {
        simpleExecutor.execute(task);
    }

    public static CompletableFuture<?>[] start(Runnable... task) {
        return Arrays.stream(task).map(AsyncService::start).toArray(CompletableFuture[]::new);
    }

    public static ScheduledFuture<?> setTimeout(Runnable runnable, long milliseconds) {
        return perTaskScheduledExecutor.schedule(runnable, milliseconds, TimeUnit.MILLISECONDS);
    }

    public static <T> ScheduledFuture<T> setTimeout(Callable<T> callable, long milliseconds) {
        return perTaskScheduledExecutor.schedule(callable, milliseconds, TimeUnit.MILLISECONDS);
    }

    public static <T> T syncRun(Supplier<T> task, long milliseconds) throws TimeoutException {
        try {
            return CompletableFuture.supplyAsync(task, service()).get(milliseconds, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new TimeoutException();
        } catch (ExecutionException e) {
            throw ((RuntimeException) e.getCause());
        }
    }

    public static void syncRun(Runnable task, long milliseconds) throws TimeoutException {
        try {
            CompletableFuture.runAsync(task, service()).get(milliseconds, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new TimeoutException();
        } catch (ExecutionException e) {
            throw ((RuntimeException) e.getCause());
        }
    }

    public static ScheduledFuture<?> setInterval(Runnable runnable, long milliseconds) {
        return setInterval(runnable, 0, milliseconds);
    }

    public static ScheduledFuture<?> setInterval(Runnable runnable, long milliseconds, long initialDelayMilliseconds) {
        return perTaskScheduledExecutor.scheduleAtFixedRate(runnable, initialDelayMilliseconds, milliseconds, TimeUnit.MILLISECONDS);
    }
}
