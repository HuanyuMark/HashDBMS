package org.hashdb.ms.persistent;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

/**
 * Date: 2024/2/28 13:28
 *
 * @author Huanyu Mark
 */
public class NoFlusher implements AofFileFlusher {

    private final static NoFlusher INSTANCE = new NoFlusher();

    public static NoFlusher get() {
        return INSTANCE;
    }

    @Override
    public void append(CharSequence command) {

    }

    @Override
    public void append(ByteBuf commandBuf) {

    }

    @Override
    public void append(ByteBuffer commandBuf) {

    }
}
