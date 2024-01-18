package org.hashdb.ms.net.exception;

import lombok.experimental.StandardException;
import org.hashdb.ms.exception.DBClientException;
import org.hashdb.ms.net.nio.msg.v1.ErrorMessage;

/**
 * Date: 2023/12/1 15:30
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@StandardException
public class IllegalMessageException extends DBClientException {
    public static final ErrorMessage MSG = new ErrorMessage(new IllegalMessageException("illegal message"));
}
