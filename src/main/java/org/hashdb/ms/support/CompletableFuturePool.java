package org.hashdb.ms.support;

import java.lang.constant.Constable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Date: 2024/3/6 16:16
 *
 * @author Huanyu Mark
 */
public class CompletableFuturePool {

    private static final Map<Constable, CompletableFuture<?>> pool = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> get(Constable result) {
        return (CompletableFuture<T>) pool.computeIfAbsent(result, CompletableFuture::completedFuture);
    }
}
