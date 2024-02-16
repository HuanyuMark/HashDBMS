package org.hashdb.ms.net.nio.msg.v1;

import org.jetbrains.annotations.NotNull;

/**
 * Date: 2024/1/17 16:15
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class AuthenticationMessage extends Message<AuthenticationMessage.Body> {
    public AuthenticationMessage(long id, Body body) {
        super(id, body);
    }

    public AuthenticationMessage(Body body) {
        super(body);
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.AUTHENTICATION;
    }

    public record Body(String username, String password) {
    }

    @Override
    public @NotNull AuthenticationMessage.Body body() {
        return body;
    }
}
