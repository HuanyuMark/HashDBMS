package org.hashdb.ms.net.nio;

import io.netty.util.AttributeKey;
import org.hashdb.ms.net.ConnectionSession;
import org.hashdb.ms.net.client.CloseMessage;

/**
 * Date: 2024/2/17 15:13
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface BaseConnectionSession extends TransientConnectionSession, ConnectionSession {

    AttributeKey<BaseConnectionSession> KEY = AttributeKey.newInstance("session");

    void onClose(TransientConnectionSession session);

    SessionMeta getSessionMeta();

    @Override
    default void close(CloseMessage closeMessage) {
        close(new org.hashdb.ms.net.nio.msg.v1.CloseMessage(0, closeMessage.getData()));
    }
}
