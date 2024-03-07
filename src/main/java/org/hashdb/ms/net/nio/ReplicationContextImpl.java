package org.hashdb.ms.net.nio;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.netty.channel.Channel;
import lombok.Getter;
import org.hashdb.ms.net.nio.msg.v1.SyncMessage;
import org.hashdb.ms.support.MasterOnly;
import org.hashdb.ms.util.AsyncService;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Date: 2024/3/6 21:02
 *
 * @author Huanyu Mark
 */
public class ReplicationContextImpl implements ReplicationContext {
    /**
     * 主节点与从节点都会有这个复制偏移量, 以表示同步的进度
     * Note: 只会被执行层修改, 所以原子
     */
    @Getter
    @JsonIgnore
    private volatile long replicationOffset;

    /**
     * Note: 只会被执行层修改, 所以原子
     */
    private volatile long discardCacheCount;
    /**
     * 主节点才会有的缓冲区, 以供从节点使用
     */
    @JsonIgnore
    private VolatileCircularFifoQueue<CharSequence> replicationCache;

    private final ReplicationGroup group;

    ReplicationContextImpl(ReplicationGroup group) {
        this.group = group;
        if (!group.getSlaves().all().isEmpty()) {
            replicationCache = newReplicationCache();
        }
    }

    /**
     * 主结点执行, 在执行层线程中执行
     *
     * @param command 要从节点执行的命令
     */
    @Override
    @MasterOnly
    public void cache(String command) {
        if (replicationCache != null) {
            replicationCache.add(command);
        }
        // 单写者多读者场景下, 只需确保可见性
        ++replicationOffset;
        // broadcast to all salves
        AsyncService.start(() -> {
            var offset = replicationOffset;
            for (var slave : group.getSlaves().all()) {
                slave.channel().addListener(future -> {
                    if (!future.isSuccess()) {
                        return;
                    }
                    Channel channel = (Channel) future.get();
                    channel.writeAndFlush(new SyncMessage(offset, command));
                });
            }
        });
    }

    /**
     * 这个方法在网络层执行, 与执行{@link ReplicationContext#cache}的线程不是同一个线程, 会有并发问题
     * <p>
     * 批量的从缓冲区中复制出需要增量同步的命令序列, 不保证期间复制出来的命令的正确性
     * (极端情况下会出现数据错误)
     *
     * @param slaveReplicationOffset 从节点的复制偏移量
     * @return 如果为null, 说明这个偏移量无效, 需要执行全量同步
     * 正常返回的一个链表,则表示要执行的增量同步序列
     */
    @Override
    public String[] getIncrSyncSequence(long slaveReplicationOffset) {
        // 理论上, 从节点偏移量不可能大于主结点的偏移量
        var delta = replicationOffset - slaveReplicationOffset;
        if (delta >= group.getReplicationCacheSize() || delta < 0) {
            return null;
        }

        var syncSequence = new String[Math.min((int) (delta), 50)];
        // 极端情况下, 从缓冲区中获得的元素为下n个轮回(循环队列的start,end指针完整走n轮又回到当前位置)
        // 相同索引处的元素就会不同
        // 只要缓冲区足够大, 这种情况的发生概率就会很低
        for (int i = 0; i < syncSequence.length; i++) {
            // 理论上, 这个get方法不会返回null, 因为replicationOffset 一定比 slaveReplicationOffset 大
            syncSequence[i] = replicationCache.get(i + (int) (slaveReplicationOffset - discardCacheCount)).toString();
        }
        return syncSequence;
    }

    @NotNull
    private VolatileCircularFifoQueue<CharSequence> newReplicationCache() {
        return new VolatileCircularFifoQueue<>(group.getReplicationCacheSize()) {
            @Override
            protected void discard() {
                ++discardCacheCount;
            }
        };
    }

    /**
     * 单一写者, 多读者 FIFO 循环队列. 多线程具有可见性
     */
    private static class VolatileCircularFifoQueue<E> {
        protected final AtomicReferenceArray<E> elements;
        protected volatile int start;
        protected volatile int end;
        protected final int capacity;
        protected volatile boolean full = false;

        private VolatileCircularFifoQueue(int capacity) {
            this.elements = new AtomicReferenceArray<>(capacity);
            this.capacity = capacity;
        }

        /**
         * 单一写者调用
         */
        public void add(E el) {
            if (isAtFullCapacity()) {
                remove();
            }
            elements.set(end++, el);

            if (end >= capacity) {
                end = 0;
            }

            if (end == start) {
                full = true;
            }
        }

        /**
         * 多读者
         */
        public E get(final int index) {
            final int sz = size();
            if (index < 0 || index >= sz) {
                throw new NoSuchElementException(
                        String.format("The specified index (%1$d) is outside the available range [0, %2$d)",
                                index, sz));
            }

            final int idx = (start + index) % capacity;
            return elements.get(idx);
        }

        private E remove() {
            final E element = elements.get(start);
            if (null == element) {
                return null;
            }
            // 直接覆盖就行
//                elements[start++] = null;
            start++;
            if (start >= capacity) {
                start = 0;
            }
            full = false;
            discard();
            return element;
        }

        protected void discard() {
        }

        public boolean isAtFullCapacity() {
            return size() == capacity;
        }

        public int size() {
            int size;

            if (end < start) {
                size = capacity - start + end;
            } else if (end == start) {
                size = full ? capacity : 0;
            } else {
                size = end - start;
            }

            return size;
        }

        public boolean isEmpty() {
            return size() == 0;
        }

        public void clear() {
            int length = elements.length();
            for (int i = 0; i < length; i++) {
                elements.set(i, null);
            }
            start = end = 0;
            full = false;
        }
    }

    @Override
    public void close() throws Exception {
        replicationCache.clear();
    }
}
