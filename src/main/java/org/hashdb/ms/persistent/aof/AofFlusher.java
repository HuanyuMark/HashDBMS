package org.hashdb.ms.persistent.aof;

import io.netty.buffer.ByteBuf;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Date: 2024/2/28 13:06
 *
 * @author Huanyu Mark
 */
public interface AofFlusher extends Closeable {

    default void lock() {
    }

    void append(CharSequence command);

    void append(ByteBuf commandBuf);

    void append(ByteBuffer commandBuf);

    /**
     * 同步. 将缓冲区的命令写入硬盘
     */
    CompletableFuture<Boolean> flush();
}
