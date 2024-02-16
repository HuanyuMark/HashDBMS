package org.hashdb.ms.exception;

import lombok.experimental.StandardException;

/**
 * Date: 2023/11/22 0:24
 * 专门用来在Worker线程中包装 {@link InterruptedException} 的类，也用来打断当前Worker线程的执行
 *
 * @author huanyuMake-pecdle
 */
@StandardException
public class WorkerInterruptedException extends DBClientException {
}
