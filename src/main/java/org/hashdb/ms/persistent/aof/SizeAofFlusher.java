package org.hashdb.ms.persistent.aof;

import org.hashdb.ms.support.CompletableFuturePool;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Date: 2024/2/28 13:28
 *
 * @author Huanyu Mark
 */
public class SizeAofFlusher extends AbstractAofFlusher {

    private final int maxCacheSize;

    public SizeAofFlusher(Aof file, int maxCacheSize) throws IOException {
        super(file);
        this.maxCacheSize = maxCacheSize;
    }

    @Override
    public CompletableFuture<Boolean> flush() {
        if (buffer.readableBytes() > maxCacheSize) {
            doFlush();
        }
        if (distFileRewritable()) {
            return doRewrite();
        }
        return CompletableFuturePool.get(true);
    }
}
