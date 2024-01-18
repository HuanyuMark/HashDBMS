package org.hashdb.ms.net.nio.msg.v1;

/**
 * Date: 2024/1/17 16:18
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ActAuthenticationMessage extends ActMessage<ActAuthenticationMessage.Body> {
    public ActAuthenticationMessage(long id, long actId, Body body) {
        super(id, actId, body);
    }

    public ActAuthenticationMessage(long actId, boolean success, String msg, String username) {
        super(actId, new Body(msg, success, username));
    }

    @Override
    public MessageType type() {
        return MessageType.ACT_AUTHENTICATION;
    }

    public record Body(String msg, boolean success, String username) {
    }

    private ActMessage<Body> actId(long actId) {
        throw new UnsupportedOperationException();
    }
}
