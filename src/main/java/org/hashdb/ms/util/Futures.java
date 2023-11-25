package org.hashdb.ms.util;

import org.hashdb.ms.exception.DBInnerException;
import org.hashdb.ms.exception.WorkerInterruptedException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Date: 2023/11/22 12:10
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class Futures {
    public static <T> T unwrap(Future<T> future) {
        if(future instanceof CompletableFuture<T> cf) {
            return cf.join();
        }
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new WorkerInterruptedException(e);
        } catch (ExecutionException e) {
            throw new DBInnerException(e);
        }
    }
}
