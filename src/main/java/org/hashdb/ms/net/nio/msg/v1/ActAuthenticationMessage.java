package org.hashdb.ms.net.nio.msg.v1;

import org.jetbrains.annotations.NotNull;

/**
 * Date: 2024/1/17 16:18
 *
 * @author Huanyu Mark
 */
public class ActAuthenticationMessage extends ActMessage<ActAuthenticationMessage.Body> {

    public ActAuthenticationMessage(int actId, Body body) {
        super(actId, body);
    }

    public ActAuthenticationMessage(int id, int actId, Body body) {
        super(id, actId, body);
    }

    public ActAuthenticationMessage(int actId, boolean success, String msg, String username) {
        super(actId, new Body(msg, success, username));
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.ACT_AUTHENTICATION;
    }

    public record Body(String msg, boolean success, String username) {
    }

    private ActMessage<Body> actId(int actId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ActAuthenticationMessage.Body body() {
        return super.body();
    }
}
