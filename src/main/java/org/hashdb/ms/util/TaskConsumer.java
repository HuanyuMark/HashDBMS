package org.hashdb.ms.util;

import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.data.OpsTaskPriority;

import java.util.concurrent.CompletableFuture;

/**
 * Date: 2023/11/28 19:26
 *
 * @author huanyuMake-pecdle
 */
public interface TaskConsumer {
    CompletableFuture<Boolean> startConsumeOpsTask();

    CompletableFuture<Boolean> stopConsumeOpsTask();

    <T> T submitOpsTaskSync(OpsTask<T> task, OpsTaskPriority priority);

    <T> CompletableFuture<T> submitOpsTask(OpsTask<T> task, OpsTaskPriority priority);

    <T> T submitOpsTaskSync(OpsTask<T> task);

    <T> CompletableFuture<T> submitOpsTask(OpsTask<T> task);
}
