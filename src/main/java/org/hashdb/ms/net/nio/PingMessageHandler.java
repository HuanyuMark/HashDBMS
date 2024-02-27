package org.hashdb.ms.net.nio;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.hashdb.ms.net.nio.msg.v1.ServerPingMessage;

/**
 * Date: 2024/2/20 12:41
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Deprecated
@ChannelHandler.Sharable
public class PingMessageHandler extends SimpleChannelInboundHandler<ServerPingMessage> implements NamedChannelHandler {

    private static final PingMessageHandler instance = new PingMessageHandler();

    public static PingMessageHandler get() {
        return instance;
    }

    private PingMessageHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ServerPingMessage msg) throws Exception {
        ctx.write(msg.pong());
    }
}
