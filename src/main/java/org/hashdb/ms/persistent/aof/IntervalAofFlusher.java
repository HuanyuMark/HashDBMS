package org.hashdb.ms.persistent.aof;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Date: 2024/2/28 13:29
 *
 * @author Huanyu Mark
 */
@Getter
@Slf4j
public abstract class IntervalAofFlusher extends CachedAofFlusher {
    protected final long interval;

    public IntervalAofFlusher(Aof file, long msInterval) throws IOException {
        super(file);
        this.interval = msInterval;
    }
}
