package org.hashdb.ms.persistent;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

/**
 * Date: 2024/2/28 13:06
 *
 * @author Huanyu Mark
 */
public interface AofFileFlusher {

    void append(CharSequence command);

    void append(ByteBuf commandBuf);

    void append(ByteBuffer commandBuf);
}
