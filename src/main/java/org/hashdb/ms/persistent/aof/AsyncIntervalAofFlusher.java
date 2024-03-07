package org.hashdb.ms.persistent.aof;

import io.netty.buffer.ByteBuf;
import org.hashdb.ms.support.CompletableFuturePool;
import org.hashdb.ms.util.AsyncService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

/**
 * Date: 2024/2/28 23:08
 * 原本这个类不是抽象的, 按要求, 需要实现无锁的flush, 但是发现flush时有
 * 数据一致性问题, 所以就直接采用 {@link SyncIntervalAofFlusher} 的flush实现
 *
 * @author Huanyu Mark
 */
public class AsyncIntervalAofFlusher extends IntervalAofFlusher {
    private final ScheduledFuture<?> flusherTask;

    public AsyncIntervalAofFlusher(Aof file, long msInterval) throws IOException {
        super(file, msInterval);
        flusherTask = AsyncService.setInterval(() -> {
            synchronized (this) {
                for (var command : cache) {
                    writerToBuffer(command);
                }
                if (buffer.readableBytes() > 0) {
                    flush();
                }
            }
        }, msInterval);
    }

    /**
     * 锁定flusher, 如果flusher在刷入硬盘, 则阻塞, 保证数据一致性
     */
    @Override
    public synchronized void lock() {
    }

    @Override
    public void close() throws IOException {
        super.close();
        flusherTask.cancel(true);
    }

    @Override
    protected Queue<Object> newCacheQueue() {
        return new ArrayDeque<>();
    }

    @Override
    public synchronized void append(CharSequence command) {
        super.append(command);
    }

    @Override
    public synchronized void append(ByteBuf commandBuf) {
        super.append(commandBuf);
    }

    @Override
    public synchronized void append(ByteBuffer commandBuf) {
        super.append(commandBuf);
    }


    protected CompletableFuture<Boolean> asyncFlush() {
        super.flush();
        cache.clear();
        return CompletableFuturePool.get(true);
    }

    public synchronized CompletableFuture<Boolean> flush() {
        return asyncFlush();
    }
}
