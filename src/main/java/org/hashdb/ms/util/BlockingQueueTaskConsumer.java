package org.hashdb.ms.util;

import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.data.OpsTaskPriority;
import org.hashdb.ms.exception.WorkerInterruptedException;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Date: 2023/11/27 14:51
 *
 * @author Huanyu Mark
 */
public class BlockingQueueTaskConsumer implements TaskConsumer {

    protected final BlockingDeque<OpsTask<?>> opsTaskDeque = new LinkedBlockingDeque<>();
    // TODO: 2024/1/14 应该要允许用户调整这个消费者线程执行的优先级
    protected CompletableFuture<?> opsTaskConsumeLoop;
    protected final AtomicBoolean receiveNewTask = new AtomicBoolean(false);

    @Override
    public synchronized CompletableFuture<Boolean> startConsumeOpsTask() {
        // 正在接收新任务，则直接 返回启动成功
        if (receiveNewTask.get()) {
            return CompletableFuture.completedFuture(true);
        }
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Supplier<CompletableFuture<?>> opsTaskConsumerSupplier = () -> AsyncService.start(() -> {
            receiveNewTask.set(true);
            future.complete(true);
            while (true) {
                if (opsTaskDeque.isEmpty() && !receiveNewTask.get()) {
                    opsTaskConsumeLoop = null;
                    break;
                }
                try {
                    exeTask(opsTaskDeque);
                } catch (InterruptedException e) {
                    throw new WorkerInterruptedException(e);
                }
            }
        });
        // 需要接收新任务
        // 消费线程在running, 就不开启新消费任务, 返回启动成功
        // 不在running, 就开启一个新消费任务
        if (opsTaskConsumeLoop == null) {
            opsTaskConsumeLoop = opsTaskConsumerSupplier.get();
            return future;
        }
        return CompletableFuture.completedFuture(true);
    }

    protected void exeTask(BlockingDeque<OpsTask<?>> taskDeque) throws InterruptedException {
        taskDeque.take().get();
    }

    @Override
    public CompletableFuture<Boolean> stopConsumeOpsTask() {
        receiveNewTask.compareAndSet(true, false);
        if (opsTaskConsumeLoop == null) {
            return CompletableFuture.completedFuture(true);
        }
        // 避免消费者线程一直卡在 take() 方法
        opsTaskDeque.add(OpsTask.empty());
        return opsTaskConsumeLoop.thenApply(v -> true);
    }

    /**
     * 立即停止消费者线程, 并清空任务队列
     */
    public boolean shutdownConsumer() {
        opsTaskDeque.clear();
        receiveNewTask.set(false);
        if (opsTaskConsumeLoop == null) {
            return true;
        }
        opsTaskConsumeLoop.cancel(true);
        return true;
    }

    // TODO: 2024/1/4 等待消费者线程将任务队列消费完毕后, 再停止消费者线程

    public int remainingTaskSize() {
        return opsTaskDeque.size();
    }

    @Override
    public <T> T submitOpsTaskSync(OpsTask<T> task, OpsTaskPriority priority) {
        return submitOpsTask(task, priority).join();
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
