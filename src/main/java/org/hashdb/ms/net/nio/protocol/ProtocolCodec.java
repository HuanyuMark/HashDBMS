package org.hashdb.ms.net.nio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.hashdb.ms.net.exception.UnsupportedProtocolException;
import org.hashdb.ms.net.nio.msg.v1.Message;

/**
 * Date: 2024/1/17 12:10
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public enum ProtocolCodec {
    HASH_V1(new V1HashProtocolCodec()),
    ;

    static final ProtocolCodec[] constant = ProtocolCodec.values();

    final HashProtocolCodec codec;

    ProtocolCodec(HashProtocolCodec codec) {
        this.codec = codec;
    }

    public static ProtocolCodec ofCode(byte b) throws UnsupportedProtocolException {
        try {
            return constant[b];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw UnsupportedProtocolException.unsupported(b);
        }
    }

    public Message<?> decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        return codec.decode(ctx, in);
    }

    public ByteBuf encode(ChannelHandlerContext ctx, Message<?> msg) {
        return codec.encode(ctx, msg);
    }
}
