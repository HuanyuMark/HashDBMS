package org.hashdb.ms.net.nio;

import io.netty.channel.Channel;
import org.hashdb.ms.net.nio.msg.v1.CloseMessage;
import org.hashdb.ms.net.nio.protocol.Protocol;

/**
 * Date: 2024/2/1 17:28
 *
 * @author Huanyu Mark
 */
public interface TransientConnectionSession extends ChannelHook, AutoCloseable {

    int id();

    void protocol(Protocol protocol);

    Protocol protocol();

    void startInactive();

    void stopInactive();

    void close(CloseMessage closeMessage);

    String username();

    Channel channel();

    boolean isActive();

    @Override
    void close();
}
