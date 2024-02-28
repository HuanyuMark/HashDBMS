package org.hashdb.ms.compiler;

import java.util.concurrent.CompletableFuture;

/**
 * Date: 2024/1/19 0:06
 *
 * @author Huanyu Mark
 */
public interface CommandExecutor {
    CompletableFuture<Object> execute(String command);
}
