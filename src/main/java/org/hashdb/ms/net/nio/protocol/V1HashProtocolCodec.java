package org.hashdb.ms.net.nio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
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
 * @version 0.0.1
 */
@Slf4j
public class V1HashProtocolCodec implements HashProtocolCodec {

    private static final byte[] magic = {'h', 'a', 's', 'h'};

    private static final int MAGIC_NUM;

    static {
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer().writeBytes(magic);
        MAGIC_NUM = byteBuf.readInt();
        byteBuf.release();
    }

    @Override
    public @NotNull ByteBuf encode(ChannelHandlerContext ctx, Message<?> msg) {
        var body = msg.bodyParser().encode(msg.body());
        var out = ctx.alloc().buffer(body.length + 30);
        // 4 magic
        out.writeBytes(magic);
        // 1 version
        out.writeByte(msg.session().supportedProtocol().ordinal());
        // 1 deserialize method 其实这个字段可以不记录, 只需要知道消息体类型, 即type字段就知道如何解析了
        // 但是如果去掉这个byte,那么消息头的大小就变成了21,还需要padding补齐,所以干脆就这样的了
        out.writeByte(msg.bodyParser().ordinal());
        // 4 type
        out.writeInt(msg.type().ordinal());
        // 8 message id
        out.writeLong(msg.id());
        // 4 body length
        // 如果是应答类消息, 那么body前8个字节就为actId
        if (msg.type().isActMessage()) {
            // expand body length for actId
            out.writeInt(body.length + 8);
            // 4 act message id
            out.writeLong(((ActMessage<?>) msg).actId());
        } else {
            out.writeInt(body.length);
        }
        // [body length] body
        out.writeBytes(body);
        return out;
    }

    @Override
    public @Nullable Message<?> decode(ChannelHandlerContext ctx, ByteBuf in) {
        // check body parse method
        var bodyParser = HashProtocolCodec.readBodyParser(ctx, in);
        if (bodyParser == null) {
            return null;
        }
        var messageType = HashProtocolCodec.readMessageType(ctx, in);
        if (messageType == null) {
            return null;
        }
        var messageId = in.readLong();
        // 读取body长度
        in.readInt();
        // 如果是应答类消息, 那么body前8个字节就为actId
        Long actId = null;
        if (messageType.isActMessage()) {
            actId = in.readLong();
        }
        var body = readBody(ctx, in, bodyParser);
        if (body == null) {
            return null;
        }
        return messageType.create(messageId, actId, body);
    }

    protected @Nullable Object readBody(ChannelHandlerContext ctx, ByteBuf in, BodyParser bodyParser) {
        byte[] bodyBytes;
        int offset;
        int length;
        // 这个bodyBytes可能可以不需要复制,能0拷贝就0拷贝
        if (in.hasArray()) {
            log.info("MESSAGE BODY PARSE FAST");
            offset = in.readerIndex();
            length = in.writerIndex();
            bodyBytes = in.array();
        } else {
            log.info("MESSAGE BODY PARSE SLOW");
            offset = 0;
            length = in.readableBytes();
            bodyBytes = new byte[length];
        }
        in.readBytes(bodyBytes);
        Object body;
        try {
            return bodyParser.decode(bodyBytes, offset, length);
        } catch (Exception e) {
            log.warn("MESSAGE PARSE ERROR", e);
            ctx.writeAndFlush(new UnsupportedBodyTypeException("body pase error"));
            return null;
        }
    }
}
