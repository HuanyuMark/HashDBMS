package org.hashdb.ms.net.nio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.net.exception.UnsupportedBodyTypeException;
import org.hashdb.ms.net.nio.msg.v1.ActMessage;
import org.hashdb.ms.net.nio.msg.v1.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2024/1/18 18:35
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
@Deprecated
public class V1HashProtocolCodec implements ProtocolCodec {

    @Override
    public @NotNull ByteBuf encode(ChannelHandlerContext ctx, Message<?> msg) {
        var body = msg.getMeta().bodyParser().encode(msg.body());
        var out = ctx.alloc().buffer(body.readableBytes() + 1 + 30);
        // 1 protocol
        out.writeByte(msg.session().protocol().key());
        // 4 message meta(message type info)
        out.writeInt(msg.getMeta().key());
        // 8 message id
        out.writeLong(msg.id());
        // 4 body length
        // 如果是应答类消息, 那么body前8个字节就为actId
        if (msg.getMeta().isActMessage()) {
            // expand body length for actId
            out.writeInt(body.readableBytes() + 1 + 8);
            // 4 act message id
            out.writeLong(((ActMessage<?>) msg).actId());
        } else {
            out.writeInt(body.readableBytes() + 1);
        }
        // [body length] body
        out.writeBytes(body);
        return out;
    }

    @Override
    public @Nullable Message<?> decode(ChannelHandlerContext ctx, ByteBuf in) {
        // check body parse method
        var messageMeta = ProtocolCodec.resolveMessageMeta(ctx, in);
        if (messageMeta == null) {
            return null;
        }
        var id = in.readLong();
        // 略过body长度, 这个body长度是在LengthFieldBasedFrameDecoder被用到的字段
        in.readerIndex(in.readerIndex() + 4);
        // parse body
        try {
            return messageMeta.create(id, in);
        } catch (Exception e) {
            log.warn("MESSAGE PARSE ERROR", e);
            ctx.write(new UnsupportedBodyTypeException("body pase error").msg(0));
            return null;
        }
    }
}
