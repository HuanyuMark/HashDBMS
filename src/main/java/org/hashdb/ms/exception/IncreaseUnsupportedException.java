package org.hashdb.ms.exception;

import lombok.experimental.StandardException;
import org.hashdb.ms.data.DataType;

/**
 * Date: 2023/11/22 20:55
 *
 * @author Huanyu Mark
 */
@StandardException
public class IncreaseUnsupportedException extends DBClientException {
    public static IncreaseUnsupportedException of(DataType type) {
        return new IncreaseUnsupportedException("type '" + type + "' does not support increments");
    }
}
