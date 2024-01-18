package org.hashdb.ms.net.nio.msg.v1;

import org.hashdb.ms.exception.DBClientException;

/**
 * Date: 2024/1/17 14:05
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ErrorMessage extends Message<ErrorMessage.Body> {

    public ErrorMessage(long id, Body body) {
        super(id, body);
    }

    public ErrorMessage(Body body) {
        super(body);
    }

    public ErrorMessage(DBClientException e) {
        super(new Body(e.getClass().getSimpleName(), e.getCause().getMessage()));
    }

    public ErrorMessage(String cause) {
        super(new Body("Exception", cause));
    }

    @Override
    public MessageType type() {
        return MessageType.ERROR;
    }

    public record Body(
            String exception,
            String cause
    ) {
    }

}
