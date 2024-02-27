package org.hashdb.ms.net.nio;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.net.nio.protocol.Protocol;
import org.hashdb.ms.net.nio.protocol.ProtocolCodec;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: 2024/1/16 21:11
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class ClientChannelInitializer extends ChannelInitializer<NioSocketChannel> implements Closeable {

    private final SessionMountedHandler sessionMountedHandler;

    private final LoggingHandler loggingHandler = new LoggingHandler(LogLevel.DEBUG);

    private final ConnectionCountLimiter connectionCountLimiter = new ConnectionCountLimiter();

    @Override
    protected void initChannel(NioSocketChannel ch) {
        var heartbeatHandler = new HeartbeatHandler();
        var codec = new ProtocolCodec(Protocol.HASH_V1);
        ch.pipeline().addLast(connectionCountLimiter.handlerName(), connectionCountLimiter)
                .addLast(heartbeatHandler.handlerName(), heartbeatHandler)
                .addLast(NamedChannelHandler.handlerName(loggingHandler), loggingHandler)
                .addLast(codec.handlerName(), codec)
                .addLast(sessionMountedHandler.handlerName(), sessionMountedHandler)
                .addLast(UncaughtExceptionLogger.HANDLER_NAME, UncaughtExceptionLogger.instance());
    }

    @Override
    public void close() {
        sessionMountedHandler.close();
    }

    static class ConnectionCountLimiter extends ChannelInboundHandlerAdapter implements NamedChannelHandler {
        private static final AtomicInteger CONNECTION_COUNT_COUNTER = new AtomicInteger();

        private static final int MAX_CONNECTION_COUNT = HashDBMSApp.ctx().getBean(DBServerConfig.class).getMaxConnections();

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            int currentCount = CONNECTION_COUNT_COUNTER.incrementAndGet();
            if (currentCount > MAX_CONNECTION_COUNT) {
                ctx.channel().close(); // 关闭连接
            }
            ctx.fireChannelActive();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            CONNECTION_COUNT_COUNTER.decrementAndGet();
            ctx.fireChannelInactive();
        }
    }
}
