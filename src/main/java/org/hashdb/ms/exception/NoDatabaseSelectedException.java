package org.hashdb.ms.exception;

import lombok.experimental.StandardException;

/**
 * Date: 2024/1/4 21:40
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@StandardException
public class NoDatabaseSelectedException extends DBClientException {
    public static NoDatabaseSelectedException of() {
        return new NoDatabaseSelectedException("No database selected");
    }
}
