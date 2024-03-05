package org.hashdb.ms.persistent.aof;

import java.io.IOException;

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
    public void flush() {
        if (buffer.readableBytes() > maxCacheSize) {
            doFlush();
        }
        if (distFileRewritable()) {
            doRewrite();
        }
    }
}
