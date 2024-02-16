package org.hashdb.ms.net.nio.msg.v1;

import org.hashdb.ms.exception.DBClientException;

/**
 * Date: 2024/1/17 14:05
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ErrorMessage extends ActMessage<ErrorMessage.Body> {

    public ErrorMessage(long actId, Body body) {
        super(actId, body);
    }

    public ErrorMessage(Message<?> request, DBClientException e) {
        this(request.id, e);
    }

    public ErrorMessage(long actId, DBClientException e) {
        super(actId, new Body(e.getClass().getSimpleName(), e.getCause().getMessage()));
    }

    public ErrorMessage(long actId, String cause) {
        super(actId, new Body("Exception", cause));
    }

    public ErrorMessage(Message<?> request, String cause) {
        this(request.id, cause);
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.ERROR;
    }

    public record Body(
            String exception,
            String cause
    ) {
    }

}
