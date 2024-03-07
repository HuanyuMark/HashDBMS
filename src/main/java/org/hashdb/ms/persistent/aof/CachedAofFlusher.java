package org.hashdb.ms.persistent.aof;

import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.util.AsyncService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

/**
 * Date: 2024/3/3 13:59
 *
 * @author Huanyu Mark
 */
@Slf4j
public abstract class CachedAofFlusher extends AbstractAofFlusher {

    protected final Queue<Object> cache = newCacheQueue();

    public CachedAofFlusher(Aof aof) throws IOException {
        super(aof);
    }

    protected abstract Queue<Object> newCacheQueue();

    @Override
    public void append(CharSequence command) {
        cache.add(command);
    }

    @Override
    public void append(ByteBuf commandBuf) {
        cache.add(commandBuf);
    }

    @Override
    public void append(ByteBuffer commandBuf) {
        cache.add(commandBuf);
    }

    @Override
    public CompletableFuture<Boolean> flush() {
        var res = super.flush();
        var next = new CompletableFuture<Boolean>();
        res.handleAsync((ok, e) -> {
            cache.clear();
            if (e != null) {
                next.completeExceptionally(e);
                return null;
            }
            next.complete(ok);
            return null;
        }, AsyncService.service());
        return next;
    }
}
