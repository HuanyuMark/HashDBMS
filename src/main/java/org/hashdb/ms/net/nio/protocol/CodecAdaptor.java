package org.hashdb.ms.net.nio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.hashdb.ms.net.nio.NamedChannelHandler;
import org.hashdb.ms.net.nio.msg.v1.Message;

import java.util.List;

/**
 * Date: 2024/2/19 12:00
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class CodecAdaptor extends MessageToMessageCodec<ByteBuf, Message<?>> implements NamedChannelHandler {

    private Protocol protocol;

    public CodecAdaptor(Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message<?> msg, List<Object> out) throws Exception {
        out.add(protocol.codec().encode(ctx, msg));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        out.add(protocol.codec().decode(ctx, buf));
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }
}
