package org.hashdb.ms.util;

import io.netty.util.concurrent.FastThreadLocalThread;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.exception.DBSystemException;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * Date: 2024/1/18 2:13
 * 结合netty的fastThreadLocal优化与虚拟线程
 * 这个类有大bug, 虽然本线程的threadLocal都同步到虚拟线程了
 * 但是还有本线程的FastThreadLocalMap无法同步到虚拟线程
 * 需要netty本身设计的FashThreadLocalThread支持虚拟化, 将以上
 * 三个上下文变量都同步到VirtualThread里才行
 * <p>
 * 如果真需要实现携程的功能,需要使用 {@link java.lang.VirtualThread.VThreadContinuation}
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
public class VirtualFastThreadLocalThread extends FastThreadLocalThread {

    private static final Field threadLocalsAccessor;
    private static final Field inheritableThreadLocalsAccessor;
    private final Thread virtualThread;

    static {
        try {
            threadLocalsAccessor = Thread.class.getDeclaredField("threadLocals");
            inheritableThreadLocalsAccessor = Thread.class.getDeclaredField("inheritableThreadLocals");
            threadLocalsAccessor.setAccessible(true);
            inheritableThreadLocalsAccessor.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new DBSystemException(e);
        }
    }

    {
        virtualThread = Thread.ofVirtual().name("v-" + getName()).unstarted(this);
        syncThreadContext();
    }

    private void syncThreadContext() {
        try {
            Object tl = threadLocalsAccessor.get(this);
            //减少反射开销
            if (tl != null) {
                threadLocalsAccessor.set(virtualThread, tl);
            }
            Object itl = inheritableThreadLocalsAccessor.get(this);
            if (itl != null) {
                inheritableThreadLocalsAccessor.set(virtualThread, itl);
            }
        } catch (IllegalAccessException e) {
            throw new DBSystemException(e);
        }
    }

    @Override
    public void start() {
//        if (log.isDebugEnabled()) {
        log.info("vt '{}' start '{}'", virtualThread, this);
//        }
        virtualThread.start();
    }

    @Override
    public void interrupt() {
        virtualThread.interrupt();
    }

    @Override
    public boolean isInterrupted() {
        return virtualThread.isInterrupted();
    }

    @Override
    public ClassLoader getContextClassLoader() {
        return virtualThread.getContextClassLoader();
    }

    @Override
    public void setContextClassLoader(ClassLoader cl) {
        virtualThread.setContextClassLoader(cl);
    }

    @NotNull
    @Override
    public StackTraceElement @NotNull [] getStackTrace() {
        return virtualThread.getStackTrace();
    }

    @NotNull
    @Override
    public State getState() {
        return virtualThread.getState();
    }

    @Override
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return virtualThread.getUncaughtExceptionHandler();
    }

    @Override
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler ueh) {
        virtualThread.setUncaughtExceptionHandler(ueh);
    }

    public VirtualFastThreadLocalThread() {
    }

    public VirtualFastThreadLocalThread(Runnable target) {
        super(target);
    }

    public VirtualFastThreadLocalThread(ThreadGroup group, Runnable target) {
        super(group, target);
    }

    public VirtualFastThreadLocalThread(String name) {
        super(name);
    }

    public VirtualFastThreadLocalThread(ThreadGroup group, String name) {
        super(group, name);
    }

    public VirtualFastThreadLocalThread(Runnable target, String name) {
        super(target, name);
    }

    public VirtualFastThreadLocalThread(ThreadGroup group, Runnable target, String name) {
        super(group, target, name);
    }

    public VirtualFastThreadLocalThread(ThreadGroup group, Runnable target, String name, long stackSize) {
        super(group, target, name, stackSize);
    }
}
