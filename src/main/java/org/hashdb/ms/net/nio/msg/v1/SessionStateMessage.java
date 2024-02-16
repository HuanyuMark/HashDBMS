package org.hashdb.ms.net.nio.msg.v1;

import org.hashdb.ms.net.nio.TransientConnectionSession;

/**
 * Date: 2024/1/27 17:10
 *
 * @author huanyuMake-pecdle
 */
public class SessionStateMessage extends Message<SessionStateMessage.State> {
    public SessionStateMessage(long id, State body) {
        super(id, body);
    }

    public SessionStateMessage(TransientConnectionSession session) {
        super(create(session));
    }

    private static State create(TransientConnectionSession session) {
        return new State(session.id());
    }

    public record State(long id) {
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.SESSION_STATE;
    }
}
