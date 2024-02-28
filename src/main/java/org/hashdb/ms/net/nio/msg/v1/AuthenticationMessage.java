package org.hashdb.ms.net.nio.msg.v1;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.jetbrains.annotations.NotNull;

/**
 * Date: 2024/1/17 16:15
 *
 * @author Huanyu Mark
 */
public class AuthenticationMessage extends Message<AuthenticationMessage.Body> {
    public AuthenticationMessage(int id, Body body) {
        super(id, body);
    }

    public AuthenticationMessage(Body body) {
        super(body);
    }

    public AuthenticationMessage(String username, String password) {
        super(new Body(username, password));
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.AUTHENTICATION;
    }

    public record Body(
            @JsonAlias({"username", "UNAME"})
            String uname,
            @JsonAlias({"password", "PWD"})
            String pwd) {
    }

    @Override
    public @NotNull AuthenticationMessage.Body body() {
        return body;
    }
}
