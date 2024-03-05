package org.hashdb.ms.support;

import jnr.constants.platform.Signal;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.util.AsyncService;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

/**
 * Date: 2024/3/2 19:33
 * 系统调用
 *
 * @author Huanyu Mark
 */
@Slf4j
public class SystemCall {

    private static final POSIX system;

    static {
        system = POSIXFactory.getPOSIX();
    }

    /**
     * Note:
     * <ol>
     *     <li>Windows 不支持 -> 开启子线程执行(弱一致性, 需要手动同步)</li>
     *     <li>Unix-like 支持 -> 开启子进程执行(Copy-On-Write确保强一致性(内存隔离))</li>
     * </ol>
     *
     * @param task 交给子进程执行的任务, 其抛出的异常的cause如果是Exception(不是RuntimeException的子类)则会自动解包
     * @return 如果为true, 则任务执行成功, 否则失败. 如果该任务在子线程中执行, 如果失败的话
     * 会有异常抛出
     */
    public static CompletableFuture<Boolean> forkRun(Runnable task) {
        int childPid;
        try {
            childPid = system.fork();
        } catch (Exception e) {
            return AsyncService.start(task).thenApplyAsync(r -> Boolean.TRUE, AsyncService.service());
        }
        if (childPid < 0) {
            return AsyncService.start(task).thenApplyAsync(r -> Boolean.TRUE, AsyncService.service());
        }
        if (childPid == 0) {
            try {
                task.run();
                system.kill(system.getppid(), Signal.SIGUSR1.value());
                // 不调用System.exit(0)是为了不调用jvm的shutdownHook, 防止循环执行资源善后工作
                throw shutdownRudely();
            } catch (Throwable e) {
                try {
                    var logFile = File.createTempFile(STR."HashDB_system_call_failed_\{system.getppid()}", "log");
                    try (var out = new ObjectOutputStream(new FileOutputStream(logFile))) {
                        if (!(e.getCause() instanceof RuntimeException)) {
                            out.writeObject(e.getCause());
                        } else {
                            out.writeObject(e);
                        }
                    }
                } catch (IOException ex) {
                    log.error(STR."child process [PID: \{system.getpid()}] execute task failed.", e);
                }
                system.kill(system.getppid(), Signal.SIGUSR2.value());
                // 不调用System.exit(0)是为了不调用jvm的shutdownHook, 防止循环执行资源善后工作
                throw shutdownRudely();
            }
        }
        var future = new CompletableFuture<Boolean>();
        system.signal(Signal.SIGUSR1, sig -> future.complete(Boolean.TRUE));
        system.signal(Signal.SIGUSR2, sig -> {
            try {
                var logFile = new File(System.getProperty("java.io.tmpdir"), STR."HashDB_system_call_failed_\{system.getpid()}.log");
                if (logFile.exists() && logFile.isFile()) {
                    try (var in = new ObjectInputStream(Files.newInputStream(Paths.get(System.getProperty("java.io.tmpdir"), STR."HashDB_system_call_failed_\{system.getpid()}.log")))) {
                        future.completeExceptionally(((Throwable) in.readObject()));
                    }
                } else {
                    future.complete(Boolean.FALSE);
                }
            } catch (IOException ignore) {
                future.complete(Boolean.FALSE);
            } catch (ClassCastException | ClassNotFoundException e) {
                log.error(STR."child process [PID: \{childPid}] store an illegal format log file. msg: {}", e.getMessage());
            }
        });
        return future;
    }

    /**
     * 强制关闭当前进程,没有机会执行其它任务善后工作
     */
    public static Exit shutdownRudely() {
        system.kill(system.getpid(), Signal.SIGKILL.intValue());
        return Exit.normal();
    }
}
