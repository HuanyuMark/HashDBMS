package org.hashdb.ms.net.nio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.net.nio.msg.v1.Message;

import java.util.List;

/**
 * Date: 2024/1/16 21:19
 * magic version
 *
 * @author Huanyu Mark
 */
@Slf4j
public class PureHashProtocolCodec extends ByteToMessageCodec<Message<?>> {
    private static final byte[] magic = {'h', 'a', 's', 'h'};

    private static final int MAGIC_NUM = 17501140;

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {

    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

    }
}
