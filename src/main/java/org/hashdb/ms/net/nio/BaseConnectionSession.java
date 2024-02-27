package org.hashdb.ms.net.nio;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.hashdb.ms.net.ConnectionSession;
import org.hashdb.ms.net.bio.client.CloseMessage;

/**
 * Date: 2024/2/17 15:13
 *
 * @author huanyuMake-pecdle
 */
public interface BaseConnectionSession extends TransientConnectionSession, ConnectionSession {

    AttributeKey<BaseConnectionSession> KEY = AttributeKey.newInstance("session");

    default void onClose(TransientConnectionSession session) {
    }

    ;

    SessionMeta getMeta();

    @Override
    default void close(CloseMessage closeMessage) {
        close(new org.hashdb.ms.net.nio.msg.v1.CloseMessage(0, closeMessage.getData()));
    }

    default void changeChannel(Channel channel) {
        onChannelChange(channel);
    }

}
