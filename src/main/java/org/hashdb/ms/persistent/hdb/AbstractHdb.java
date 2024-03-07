package org.hashdb.ms.persistent.hdb;

import lombok.Getter;
import org.hashdb.ms.config.HdbConfig;
import org.hashdb.ms.support.CompletableFuturePool;
import org.hashdb.ms.support.StaticAutowired;

import java.util.concurrent.CompletableFuture;

/**
 * Date: 2024/3/3 13:07
 *
 * @author Huanyu Mark
 */
public abstract class AbstractHdb implements AutoCloseable {
    @Getter
    @StaticAutowired
    private static HdbConfig hdbConfig;

    protected AbstractHdb() {
    }

    public abstract void modify();

    public abstract void modify(int delta);

    public CompletableFuture<Boolean> flush() {
        return CompletableFuturePool.get(true);
    }
}
