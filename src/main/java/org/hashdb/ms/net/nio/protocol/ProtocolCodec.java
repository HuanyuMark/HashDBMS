package org.hashdb.ms.net.nio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.hashdb.ms.net.exception.IllegalMessageException;
import org.hashdb.ms.net.exception.UnsupportedBodyTypeException;
import org.hashdb.ms.net.nio.msg.v1.Message;
import org.hashdb.ms.net.nio.msg.v1.MessageMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2024/1/18 18:36
 *
 * @author huanyuMake-pecdle
 */
interface ProtocolCodec {
//    byte[] EMPTY_BODY = new byte[0];

    @Nullable Message<?> decode(ChannelHandlerContext ctx, ByteBuf in);

    @NotNull ByteBuf encode(ChannelHandlerContext ctx, Message<?> msg);

    @Nullable
    static MessageMeta resolveMessageMeta(ChannelHandlerContext ctx, ByteBuf in) {
        MessageMeta messageType;
        try {
            messageType = MessageMeta.resolve(in.readInt());
        } catch (IllegalMessageException e) {
            ctx.writeAndFlush(e.msg(0));
            return null;
        }
        return messageType;
    }

    /**
     * 不用再读取, messageType里就记录有解析body的的方法
     */
    @Deprecated
    static @Nullable BodyParser readBodyParser(ChannelHandlerContext ctx, ByteBuf in) {
        BodyParser bodyParser;
        try {
            return BodyParser.resolve(in.readByte());
        } catch (UnsupportedBodyTypeException e) {
            // 不支持的解析方法
            ctx.writeAndFlush(e.msg(0));
            return null;
        }
    }
}
