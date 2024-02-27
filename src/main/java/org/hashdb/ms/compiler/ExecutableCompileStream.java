package org.hashdb.ms.compiler;

import java.util.concurrent.CompletableFuture;

/**
 * Date: 2024/2/26 10:38
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface ExecutableCompileStream {
    CompletableFuture<Object> execute();
}
