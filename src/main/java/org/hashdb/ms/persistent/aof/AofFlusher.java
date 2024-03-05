package org.hashdb.ms.persistent.aof;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

/**
 * Date: 2024/2/28 13:06
 *
 * @author Huanyu Mark
 */
public interface AofFlusher {

    default void lock() {
    }

    void append(CharSequence command);

    void append(ByteBuf commandBuf);

    void append(ByteBuffer commandBuf);

    /**
     * 同步. 将缓冲区的命令写入硬盘
     */
    void flush();
}
