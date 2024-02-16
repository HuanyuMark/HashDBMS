package org.hashdb.ms.net.nio;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.net.nio.protocol.CodecDispatcher;

import java.io.Closeable;

/**
 * Date: 2024/1/16 21:11
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public class ClientChannelInitializer extends ChannelInitializer<NioSocketChannel> implements Closeable {

    private final CodecDispatcher messageCodec;

    private final SessionMountedHandler sessionMountedHandler;

    private final LoggingHandler loggingHandler;

    public static LengthFieldBasedFrameDecoder frameDecoder() {
        return new LengthFieldBasedFrameDecoder(20 * 1024, 18, 4, 0, 0, true);
    }

    @Override
    protected void initChannel(NioSocketChannel ch) {
        ch.pipeline().addLast(NamedChannelHandler.handlerName(LengthFieldBasedFrameDecoder.class), frameDecoder());
        ch.pipeline().addLast(NamedChannelHandler.handlerName(loggingHandler), loggingHandler);
        // 下面这个协议解析器, 需要配合帧解析器运作, 根据body长度进行消息解析, 所以如果messageCodec所定义的长度字段与上述的frameDecoder
        // 的规定不同, 则粘包粘包现象会存在, 且有frameDecoder可能会抛解析异常
        ch.pipeline().addLast(messageCodec.handlerName(), messageCodec);
        ch.pipeline().addLast(sessionMountedHandler.handlerName(), sessionMountedHandler);
        ch.pipeline().addLast(UncaughtExceptionLogger.HANDLER_NAME, UncaughtExceptionLogger.instance());
    }

    @Override
    public void close() {
        sessionMountedHandler.close();
    }
}
