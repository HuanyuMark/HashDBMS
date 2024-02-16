package org.hashdb.ms.exception;

import lombok.experimental.StandardException;
import org.hashdb.ms.net.nio.msg.v1.ErrorMessage;

/**
 * Date: 2023/11/21 12:47
 * DB 外部的错误，是客户端不当操作引发的错误
 *
 * @author huanyuMake-pecdle
 */
@StandardException
public class DBClientException extends RuntimeException {
    public ErrorMessage msg(long actId) {
        return new ErrorMessage(actId, this);
    }
}
