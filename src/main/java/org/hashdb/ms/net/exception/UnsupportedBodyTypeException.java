package org.hashdb.ms.net.exception;

import lombok.experimental.StandardException;
import org.hashdb.ms.exception.DBClientException;

/**
 * Date: 2024/1/18 1:28
 *
 * @author huanyuMake-pecdle
 */
@StandardException
public class UnsupportedBodyTypeException extends DBClientException {

    public static UnsupportedBodyTypeException unsupported(byte unsupportedBodyType) {
        return new UnsupportedBodyTypeException("unsupported body type '" + unsupportedBodyType + "'");
    }
}
