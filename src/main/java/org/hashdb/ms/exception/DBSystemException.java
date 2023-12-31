package org.hashdb.ms.exception;

import lombok.experimental.StandardException;

/**
 * Date: 2023/11/22 14:05
 * DB 内部的报错，专门针对 DB 编码时，由 DB 内部的错误引起的错误
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@StandardException
public class DBSystemException extends RuntimeException {
}
