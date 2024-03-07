package org.hashdb.ms.persistent.aof;

import io.netty.buffer.ByteBuf;
import org.hashdb.ms.support.CompletableFuturePool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Date: 2024/3/6 17:11
 *
 * @author Huanyu Mark
 */
public class NopAofFlusher implements AofFlusher {
    private static NopAofFlusher instance;

    public static AofFlusher get() {
        return instance == null ? (instance = new NopAofFlusher()) : instance;
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

    @Override
    public CompletableFuture<Boolean> flush() {
        return CompletableFuturePool.get(true);
    }

    @Override
    public void close() throws IOException {

    }
}
