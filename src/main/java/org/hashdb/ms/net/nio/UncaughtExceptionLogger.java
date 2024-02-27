package org.hashdb.ms.net.nio;

import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.exception.DBSystemException;

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

    public static Incorporator extract(ChannelPipeline pipeline) {
        var lastHandler = pipeline.removeLast();
        if (!(lastHandler instanceof UncaughtExceptionLogger)) {
            throw new DBSystemException("the last handler should be '" + UncaughtExceptionLogger.class + "'");
        }
        return () -> pipeline.addLast(HANDLER_NAME, INSTANCE);
    }

    public interface Incorporator {
        void incorporate();
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
