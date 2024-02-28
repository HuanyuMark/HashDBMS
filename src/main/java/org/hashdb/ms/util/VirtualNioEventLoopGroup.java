//package org.hashdb.ms.util;
//
//import io.netty.channel.EventLoopTaskQueueFactory;
//import io.netty.channel.SelectStrategyFactory;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.util.concurrent.EventExecutorChooserFactory;
//import io.netty.util.concurrent.RejectedExecutionHandler;
//
//import java.nio.channels.spi.SelectorProvider;
//import java.util.concurrent.Executor;
//import java.util.concurrent.ThreadFactory;
//
///**
// * Date: 2024/1/18 2:26
// * netty 的EventLoop所使用的Thread为 netty的DefaultThreadFactory创建出来的
// * 这个thread有一些自定义优化, 比如说,可以更快地访问ThreadLocal
// * 但如果使用默认的ThreadFactory, 就不能使用虚拟线程
// *
// * @author Huanyu Mark
// */
//public class VirtualNioEventLoopGroup extends NioEventLoopGroup {
//
//    @Override
//    protected ThreadFactory newDefaultThreadFactory() {
//        return new VirtualFastThreadLocalThreadFactory(getClass(), Thread.MAX_PRIORITY);
//    }
//
//
//    public VirtualNioEventLoopGroup() {
//    }
//
//    public VirtualNioEventLoopGroup(int nThreads) {
//        super(nThreads);
//    }
//
//    public VirtualNioEventLoopGroup(ThreadFactory threadFactory) {
//        super(threadFactory);
//    }
//
//    public VirtualNioEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
//        super(nThreads, threadFactory);
//    }
//
//    public VirtualNioEventLoopGroup(int nThreads, Executor executor) {
//        super(nThreads, executor);
//    }
//
//    public VirtualNioEventLoopGroup(int nThreads, ThreadFactory threadFactory, SelectorProvider selectorProvider) {
//        super(nThreads, threadFactory, selectorProvider);
//    }
//
//    public VirtualNioEventLoopGroup(int nThreads, ThreadFactory threadFactory, SelectorProvider selectorProvider, SelectStrategyFactory selectStrategyFactory) {
//        super(nThreads, threadFactory, selectorProvider, selectStrategyFactory);
//    }
//
//    public VirtualNioEventLoopGroup(int nThreads, Executor executor, SelectorProvider selectorProvider) {
//        super(nThreads, executor, selectorProvider);
//    }
//
//    public VirtualNioEventLoopGroup(int nThreads, Executor executor, SelectorProvider selectorProvider, SelectStrategyFactory selectStrategyFactory) {
//        super(nThreads, executor, selectorProvider, selectStrategyFactory);
//    }
//
//    public VirtualNioEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, SelectorProvider selectorProvider, SelectStrategyFactory selectStrategyFactory) {
//        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory);
//    }
//
//    public VirtualNioEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, SelectorProvider selectorProvider, SelectStrategyFactory selectStrategyFactory, RejectedExecutionHandler rejectedExecutionHandler) {
//        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory, rejectedExecutionHandler);
//    }
//
//    public VirtualNioEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, SelectorProvider selectorProvider, SelectStrategyFactory selectStrategyFactory, RejectedExecutionHandler rejectedExecutionHandler, EventLoopTaskQueueFactory taskQueueFactory) {
//        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory, rejectedExecutionHandler, taskQueueFactory);
//    }
//
//    public VirtualNioEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, SelectorProvider selectorProvider, SelectStrategyFactory selectStrategyFactory, RejectedExecutionHandler rejectedExecutionHandler, EventLoopTaskQueueFactory taskQueueFactory, EventLoopTaskQueueFactory tailTaskQueueFactory) {
//        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory, rejectedExecutionHandler, taskQueueFactory, tailTaskQueueFactory);
//    }
//
//
//}
