package org.hashdb.ms.data;

import org.hashdb.ms.util.TaskConsumer;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Date: 2023/11/28 19:21
 * 数据库视图, 根据给定的模式串, 筛选一遍key, 再将操作代理给原数据库
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class DatabaseView implements IDatabase, TaskConsumer {
    protected final Pattern pattern;

    protected final Database database;

    public DatabaseView(Database database, Pattern pattern) {
        this.pattern = pattern;
        this.database = database;
    }

    @Override
    public CompletableFuture<Boolean> startConsumeOpsTask() {
        return database.startConsumeOpsTask();
    }

    @Override
    public CompletableFuture<Boolean> stopConsumeOpsTask() {
        return database.stopConsumeOpsTask();
    }

    @Override
    public <T> T submitOpsTaskSync(OpsTask<T> task, OpsTaskPriority priority) {
        return database.submitOpsTaskSync(task, priority);
    }

    @Override
    public <T> CompletableFuture<T> submitOpsTask(OpsTask<T> task, OpsTaskPriority priority) {
        return database.submitOpsTask(task, priority);
    }

    @Override
    public <T> T submitOpsTaskSync(OpsTask<T> task) {
        return database.submitOpsTaskSync(task);
    }

    @Override
    public <T> CompletableFuture<T> submitOpsTask(OpsTask<T> task) {
        return database.submitOpsTask(task);
    }
}
