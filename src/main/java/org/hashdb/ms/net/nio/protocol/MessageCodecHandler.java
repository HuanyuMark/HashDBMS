package org.hashdb.ms.net.nio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.net.exception.UnsupportedProtocolException;
import org.hashdb.ms.net.nio.msg.v1.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Date: 2024/1/16 21:19
 * 请确保收到的byteBuf完整, 不能出现粘包半包
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class MessageCodecHandler extends MessageToMessageCodec<ByteBuf, Message<?>> {
    private static final byte[] magic = {'h', 'a', 's', 'h'};

    private static final int MAGIC_NUM;

    static {
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer().writeBytes(magic);
        MAGIC_NUM = byteBuf.readInt();
        byteBuf.release();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message<?> msg, List<Object> outList) throws Exception {
        ByteBuf out = msg.session().supportedProtocol().encode(ctx, msg);
        outList.add(out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // check message
        if (in.readableBytes() < 22 || in.readInt() != MAGIC_NUM) {
            var bytes = new byte[Math.min(in.readableBytes(), 90)];
            in.readBytes(bytes);
            log.warn("illegal message. buf: {} content: '{}'", in, new String(bytes));
            in.release();
            return;
        }
        // check version
        ProtocolCodec protocolCodec;
        try {
            protocolCodec = ProtocolCodec.ofCode(in.readByte());
        } catch (UnsupportedProtocolException e) {
            // 不支持的协议版本
            ctx.writeAndFlush(e.msg());
            return;
        }
        var message = protocolCodec.decode(ctx, in);
        out.add(message);
        in.release();
        log.info("MESSAGE PARSE {}", message);
    }
}
