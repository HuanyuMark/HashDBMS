package org.hashdb.ms.net.nio;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import org.hashdb.ms.net.exception.UnsupportedProtocolException;
import org.hashdb.ms.net.nio.msg.v1.ActMessage;
import org.hashdb.ms.net.nio.msg.v1.ProtocolSwitchingMessage;
import org.hashdb.ms.net.nio.protocol.Protocol;

import java.util.Objects;

/**
 * Date: 2024/2/3 21:43
 *
 * @author Huanyu Mark
 */
@RequiredArgsConstructor
class ProtocolSwitchingHandler extends SimpleChannelInboundHandler<ProtocolSwitchingMessage> implements NamedChannelHandler {

    private final TransientConnectionSession session;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProtocolSwitchingMessage msg) {
        var protocolCode = Objects.requireNonNullElse(msg.body(), -1);
        try {
            session.protocol(Protocol.resolve(protocolCode));
        } catch (UnsupportedProtocolException e) {
            ctx.write(e.msg(msg.id()));
            return;
        }
        ctx.write(ActMessage.act(msg.id()));
    }
}
