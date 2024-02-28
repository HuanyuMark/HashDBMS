package org.hashdb.ms.exception;

import lombok.experimental.StandardException;
import org.hashdb.ms.net.nio.msg.v1.ErrorMessage;
import org.hashdb.ms.net.nio.msg.v1.Message;

/**
 * Date: 2023/11/21 12:47
 * DB 外部的错误，是客户端不当操作引发的错误
 *
 * @author Huanyu Mark
 */
@StandardException
public class DBClientException extends RuntimeException {
    public ErrorMessage msg(int actId) {
        return new ErrorMessage(actId, this);
    }

    public ErrorMessage msg(Message<?> request) {
        return msg(request.id());
    }
}
