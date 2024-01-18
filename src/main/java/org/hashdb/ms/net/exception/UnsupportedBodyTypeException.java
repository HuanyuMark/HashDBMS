package org.hashdb.ms.net.exception;

import lombok.experimental.StandardException;
import org.hashdb.ms.exception.DBClientException;
import org.hashdb.ms.net.nio.msg.v1.ErrorMessage;

/**
 * Date: 2024/1/18 1:28
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@StandardException
public class UnsupportedBodyTypeException extends DBClientException {

    public static final ErrorMessage MSG = new ErrorMessage(new UnsupportedBodyTypeException("unsupported body type"));

    public static UnsupportedBodyTypeException unsupported(byte unsupportedBodyType) {
        return new UnsupportedBodyTypeException("unsupported body type '" + unsupportedBodyType + "'");
    }
}
