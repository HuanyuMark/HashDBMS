package org.hashdb.ms.exception;

import lombok.experimental.StandardException;

/**
 * Date: 2023/11/21 17:17
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@StandardException
public class NotFoundDatabaseException extends DBExternalException {
    public static NotFoundDatabaseException of(String databaseName) {
        return new NotFoundDatabaseException("not found database: '"+databaseName+"'");
    }
}
