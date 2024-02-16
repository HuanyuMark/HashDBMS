package org.hashdb.ms.compiler;

import java.util.concurrent.CompletableFuture;

/**
 * Date: 2024/1/19 0:06
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface CommandExecutor {
    CompletableFuture<Object> execute(String command);
}
