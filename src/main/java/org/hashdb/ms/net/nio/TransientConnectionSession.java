package org.hashdb.ms.net.nio;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.hashdb.ms.net.nio.msg.v1.CloseMessage;
import org.hashdb.ms.net.nio.protocol.Protocol;

/**
 * Date: 2024/2/1 17:28
 *
 * @author huanyuMake-pecdle
 */
public interface TransientConnectionSession extends ChannelHook, AutoCloseable {
    AttributeKey<TransientConnectionSession> KEY = AttributeKey.newInstance("session");

    long id();

    void protocol(Protocol protocol);

    Protocol protocol();

    Channel channel();

    void startInactive();

    void stopInactive();

    void close(CloseMessage closeMessage);

    String username();

    boolean isActive();

    @Override
    void close();
}
