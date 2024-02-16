package org.hashdb.ms.net.nio;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;

/**
 * Date: 2024/1/31 0:50
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
@ChannelHandler.Sharable
public class UncaughtExceptionLogger extends ChannelDuplexHandler implements NamedChannelHandler {
    private static final UncaughtExceptionLogger INSTANCE = new UncaughtExceptionLogger();

    public static final String HANDLER_NAME = INSTANCE.handlerName();

    public static UncaughtExceptionLogger instance() {
        return INSTANCE;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("INBOUND channel: {} UncaughtException", ctx.channel(), cause);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        ctx.write(msg, promise.addListener(future -> {
            if (future.isSuccess()) {
                return;
            }
            log.error("OUTBOUND channel: {} UncaughtException", ctx.channel(), future.cause());
        }));
    }
}
