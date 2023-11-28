package org.hashdb.ms.util;

import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.data.OpsTaskPriority;
import org.hashdb.ms.exception.WorkerInterruptedException;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Date: 2023/11/27 14:51
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class BlockingQueueTaskConsumer implements TaskConsumer {

    protected final BlockingDeque<OpsTask<?>> opsTaskDeque = new LinkedBlockingDeque<>();
    protected final AtomicReference<CompletableFuture<?>> opsTaskConsumeLoop = new AtomicReference<>();
    protected final AtomicBoolean receiveNewTask = new AtomicBoolean(false);

    @Override
    public CompletableFuture<Boolean> startConsumeOpsTask() {
        // 正在接收新任务，则直接 返回启动成功
        if (receiveNewTask.get()) {
            return CompletableFuture.completedFuture(true);
        }
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Supplier<CompletableFuture<?>> opsTaskConsumerSupplier = () -> AsyncService.submit(() -> {
            receiveNewTask.set(true);
            future.complete(true);
            while (true) {
                if (opsTaskDeque.size() == 0 && receiveNewTask.compareAndSet(false, false)) {
                    opsTaskConsumeLoop.set(null);
                    break;
                }
                OpsTask<?> task;
                try {
                    task = opsTaskDeque.take();
                } catch (InterruptedException e) {
                    throw new WorkerInterruptedException(e);
                }
                task.get();
            }
        });
        // 需要接收新任务
        // 消费线程在running, 就不开启新消费任务, 返回启动成功
        // 不在running, 就开启一个新消费任务
        if (opsTaskConsumeLoop.compareAndSet(null, opsTaskConsumerSupplier.get())) {
            return future;
        }
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> stopConsumeOpsTask() {
        if (!receiveNewTask.compareAndSet(true, false)) {
            return CompletableFuture.completedFuture(true);
        }
        // 避免消费者线程一直卡在 take() 方法
        opsTaskDeque.add(OpsTask.empty());
        CompletableFuture<?> consumeLoopResult = opsTaskConsumeLoop.get();
        if (consumeLoopResult == null) {
            return CompletableFuture.completedFuture(true);
        }
        return consumeLoopResult.thenApply(v -> true);
    }

    @Override
    public <T> T submitOpsTaskSync(OpsTask<T> task, OpsTaskPriority priority) {
        checkTaskQueueConsumer();
        if (OpsTaskPriority.LOW == priority) {
            opsTaskDeque.add(task);
        } else {
            opsTaskDeque.addFirst(task);
        }
        return task.result();
    }

    @Override
    public <T> CompletableFuture<T> submitOpsTask(OpsTask<T> task, OpsTaskPriority priority) {
        checkTaskQueueConsumer();
        if (OpsTaskPriority.LOW == priority) {
            opsTaskDeque.add(task);
        } else {
            opsTaskDeque.addFirst(task);
        }
        return task.future();
    }

    @Override
    public <T> T submitOpsTaskSync(OpsTask<T> task) {
        return submitOpsTaskSync(task, OpsTaskPriority.LOW);
    }

    @Override
    public <T> CompletableFuture<T> submitOpsTask(OpsTask<T> task) {
        return submitOpsTask(task, OpsTaskPriority.LOW);
    }

    /**
     * @return true 则继续接受新任务, false 则停止接受新任务
     */
    protected boolean checkTaskQueueConsumer() {
        return receiveNewTask.get();
    }
}
