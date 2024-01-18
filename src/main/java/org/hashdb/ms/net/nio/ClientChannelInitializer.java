package org.hashdb.ms.net.nio;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.logging.LoggingHandler;
import lombok.RequiredArgsConstructor;
import org.hashdb.ms.net.nio.msg.v1.Message;

/**
 * Date: 2024/1/16 21:11
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@RequiredArgsConstructor
public class ClientChannelInitializer extends ChannelInitializer<NioSocketChannel> {

    private final MessageToMessageCodec<ByteBuf, Message<?>> messageCodec;

    private final SessionFactoryHandler sessionFactoryHandler;

    private final LoggingHandler loggingHandler;

    private final ChannelInboundHandlerAdapter unresolvedMsgInterceptor = new ChannelInboundHandlerAdapter() {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!(msg instanceof Message<?>)) {
                return;
            }
            ctx.close();
        }
    };

    private static LengthFieldBasedFrameDecoder frameDecoder() {
        return new LengthFieldBasedFrameDecoder(20 * 1024, 18, 4, 0, 0, true);
    }

    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
        ch.pipeline().addLast("LoggingHandler", loggingHandler);
        ch.pipeline().addLast("FrameDecoder", frameDecoder());
        // 下面这个协议解析器, 需要配合帧解析器运作, 根据body长度进行消息解析, 所以如果messageCodec所定义的长度字段与上述的frameDecoder
        // 的规定不同, 则粘包粘包现象会存在, 且有frameDecoder可能会抛解析异常
        ch.pipeline().addLast("MessageCodec", messageCodec);
        // 拦截未被解析的byte数据
        ch.pipeline().addLast("UnresolvedMsgInterceptor", unresolvedMsgInterceptor);
        ch.pipeline().addLast("SessionFactory", sessionFactoryHandler);
    }
}
