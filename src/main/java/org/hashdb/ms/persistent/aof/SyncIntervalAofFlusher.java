package org.hashdb.ms.persistent.aof;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.support.CompletableFuturePool;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Date: 2024/2/28 23:11
 *
 * @author Huanyu Mark
 */
@Slf4j
public class SyncIntervalAofFlusher extends AsyncIntervalAofFlusher {

    public SyncIntervalAofFlusher(Aof file, long msInterval) throws IOException {
        super(file, msInterval);
    }

    @Override
    protected CompletableFuture<Boolean> asyncFlush() {
        super.asyncFlush().join();
        return CompletableFuturePool.get(true);
    }
}
