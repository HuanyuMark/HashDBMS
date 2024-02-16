package org.hashdb.ms.net.nio;

import io.netty.channel.ChannelHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Date: 2024/1/31 1:03
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface NamedChannelHandler {
    static String handlerName(Class<? extends ChannelHandler> handlerClass) {
        return handlerClass.getSimpleName();
    }

    static String handlerName(ChannelHandler handler) {
        return handler.getClass().getSimpleName();
    }

    default @NotNull String handlerName() {
        return getClass().getSimpleName();
    }
}
