package org.hashdb.ms.net.exception;

import lombok.experimental.StandardException;
import org.hashdb.ms.exception.DBClientException;

/**
 * Date: 2024/1/18 1:36
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@StandardException
public class UnsupportedProtocolException extends DBClientException {
    public static UnsupportedBodyTypeException unsupported(byte unsupportedBodyType) {
        return new UnsupportedBodyTypeException("unsupported protocol '" + unsupportedBodyType + "'");
    }
}
