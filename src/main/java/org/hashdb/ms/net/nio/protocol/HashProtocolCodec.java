package org.hashdb.ms.net.nio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.hashdb.ms.net.exception.IllegalMessageException;
import org.hashdb.ms.net.exception.UnsupportedBodyTypeException;
import org.hashdb.ms.net.nio.msg.v1.Message;
import org.hashdb.ms.net.nio.msg.v1.MessageType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2024/1/18 18:36
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
interface HashProtocolCodec {
    @Nullable Message<?> decode(ChannelHandlerContext ctx, ByteBuf in);

    @Nullable
    static MessageType readMessageType(ChannelHandlerContext ctx, ByteBuf in) {
        MessageType messageType;
        try {
            messageType = MessageType.ofCode(in.readInt());
        } catch (IllegalMessageException e) {
            ctx.writeAndFlush(e.msg());
            return null;
        }
        return messageType;
    }

    static @Nullable BodyParser readBodyParser(ChannelHandlerContext ctx, ByteBuf in) {
        BodyParser bodyParser;
        try {
            return BodyParser.ofCode(in.readByte());
        } catch (UnsupportedBodyTypeException e) {
            // 不支持的解析方法
            ctx.writeAndFlush(e.msg());
            return null;
        }
    }

    @NotNull ByteBuf encode(ChannelHandlerContext ctx, Message<?> msg);
}
