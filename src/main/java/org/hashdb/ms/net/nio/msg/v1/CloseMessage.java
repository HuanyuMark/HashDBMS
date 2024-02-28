package org.hashdb.ms.net.nio.msg.v1;

import org.hashdb.ms.exception.DBClientException;

/**
 * Date: 2024/1/17 16:21
 *
 * @author Huanyu Mark
 */
public class CloseMessage extends ErrorMessage {

    public CloseMessage(int id, String body) {
        super(id, body);
    }

    public CloseMessage(Message<?> request, String cause) {
        super(request, cause);
    }

    public CloseMessage(int actId, Body body) {
        super(actId, body);
    }

    public CloseMessage(Message<?> request, DBClientException e) {
        super(request, e);
    }

    public CloseMessage(int actId, DBClientException e) {
        super(actId, e);
    }

    @Override
    public MessageMeta getMeta() {
        return null;
    }
}
