package org.hashdb.ms.net.nio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.net.exception.UnsupportedProtocolException;
import org.hashdb.ms.net.nio.NamedChannelHandler;
import org.hashdb.ms.net.nio.msg.v1.Message;

import java.util.List;

/**
 * Date: 2024/1/16 21:19
 * 请确保收到的byteBuf完整, 不能出现粘包半包
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
@Deprecated
@ChannelHandler.Sharable
public class CodecDispatcher extends MessageToMessageCodec<ByteBuf, Message<?>> implements NamedChannelHandler {
    @Override
    protected void encode(ChannelHandlerContext ctx, Message<?> msg, List<Object> outList) {
        var out = msg.session().protocol().codec().encode(ctx, msg);
        outList.add(out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // check message
        if (in.readableBytes() < 17) {
            var bytes = new byte[Math.min(in.readableBytes(), 90)];
            in.readBytes(bytes);
            log.warn("illegal message. buf: {} content: '{}'", in, new String(bytes));
            return;
        }
        // check protocol
        Protocol protocol;
        try {
            protocol = Protocol.resolve(in.readByte());
        } catch (UnsupportedProtocolException e) {
            // 不支持的协议
            ctx.write(e.msg(0));
            return;
        }
        var message = protocol.codec().decode(ctx, in);
        if (message != null) {
            out.add(message);
        }
        log.info("MESSAGE PARSE {}", message);
    }
}
