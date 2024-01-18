package org.hashdb.ms.util;

import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * Date: 2024/1/18 2:29
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class VirtualFastThreadLocalThreadFactory extends DefaultThreadFactory {

    @Override
    protected Thread newThread(Runnable r, String name) {
        return new VirtualFastThreadLocalThread(threadGroup, r, name);
    }

    public VirtualFastThreadLocalThreadFactory(Class<?> poolType) {
        super(poolType);
    }

    public VirtualFastThreadLocalThreadFactory(String poolName) {
        super(poolName);
    }

    public VirtualFastThreadLocalThreadFactory(Class<?> poolType, boolean daemon) {
        super(poolType, daemon);
    }

    public VirtualFastThreadLocalThreadFactory(String poolName, boolean daemon) {
        super(poolName, daemon);
    }

    public VirtualFastThreadLocalThreadFactory(Class<?> poolType, int priority) {
        super(poolType, priority);
    }

    public VirtualFastThreadLocalThreadFactory(String poolName, int priority) {
        super(poolName, priority);
    }

    public VirtualFastThreadLocalThreadFactory(Class<?> poolType, boolean daemon, int priority) {
        super(poolType, daemon, priority);
    }

    public VirtualFastThreadLocalThreadFactory(String poolName, boolean daemon, int priority, ThreadGroup threadGroup) {
        super(poolName, daemon, priority, threadGroup);
    }

    public VirtualFastThreadLocalThreadFactory(String poolName, boolean daemon, int priority) {
        super(poolName, daemon, priority);
    }

}
