package org.hashdb.ms.net.exception;

import lombok.experimental.StandardException;
import org.hashdb.ms.exception.DBClientException;

/**
 * Date: 2024/1/18 1:36
 *
 * @author Huanyu Mark
 */
@StandardException
public class UnsupportedProtocolException extends DBClientException {
    public static UnsupportedBodyTypeException unsupported(int unsupportedBodyType) {
        return new UnsupportedBodyTypeException("unsupported protocol '" + unsupportedBodyType + "'");
    }
}
