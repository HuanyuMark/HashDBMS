package org.hashdb.ms.handler;

import java.util.concurrent.CompletableFuture;

/**
 * Date: 2023/11/22 0:38
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface AsyncOpsConsumer<I,O> extends OpsConsumer<I, CompletableFuture<O>>{
}
