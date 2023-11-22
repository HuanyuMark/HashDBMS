package org.hashdb.ms.util;

import org.hashdb.ms.exception.WorkerInterruptedException;

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
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new WorkerInterruptedException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
