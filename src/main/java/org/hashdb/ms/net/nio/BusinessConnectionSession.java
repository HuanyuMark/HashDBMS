package org.hashdb.ms.net.nio;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.CompileStream;
import org.hashdb.ms.config.CommandCacheConfig;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.net.Parameter;
import org.hashdb.ms.net.exception.MaxConnectionException;
import org.hashdb.ms.net.nio.msg.v1.*;
import org.hashdb.ms.util.CacheMap;
import org.hashdb.ms.util.JsonService;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: 2024/1/16 21:15
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
public class BusinessConnectionSession extends ChannelSession {
    private static final AtomicInteger connectionCount = new AtomicInteger(0);
    @JsonProperty
    private final long id = SessionMeta.nextId();

    public BusinessConnectionSession() {
        var config = dbServerConfig.get();
        if (connectionCount.incrementAndGet() > config.getMaxConnections()) {
            connectionCount.decrementAndGet();
            throw new MaxConnectionException("too many connections");
        }
        var commandCacheConfig = config.getCommandCache();
        localCommandCache = new CacheMap<>(commandCacheConfig.getAliveDuration(), commandCacheConfig.getCacheSize());
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    protected Logger logger() {
        return log;
    }

    @Override
    public void onChannelActive(Channel channel) {
        this.channel = channel;
        var authenticationHandler = authenticationHandlerLazy.get();
        var commandExecuteHandler = commandExecuteHandlerLazy.get();
        var protocolSwitchingHandler = protocolSwitchingHandlerLazy.get();
        channel.pipeline()
                .addBefore(UncaughtExceptionLogger.HANDLER_NAME, authenticationHandler.handlerName(), authenticationHandler)
                .addBefore(UncaughtExceptionLogger.HANDLER_NAME, commandExecuteHandler.handlerName(), commandExecuteHandler)
                .addBefore(UncaughtExceptionLogger.HANDLER_NAME, protocolSwitchingHandler.handlerName(), protocolSwitchingHandler);
    }

    @Override
    public void close(CloseMessage closeMessage) {
        if(closed) {
            return;
        }
        super.close(closeMessage);
        connectionCount.decrementAndGet();
    }

    @Override
    public void onChannelChange(Channel channel) {
        onChannelActive(channel);
        authenticationHandlerLazy.get().startCheckHeartbeat(channel);
    }

    @Override
    public void onReleaseChannel() {
        channel.pipeline().remove(AuthenticationHandler.class);
        try (var commandExecuteHandler = channel().pipeline().remove(CommandExecuteHandler.class)) {
            channel().pipeline().remove()
        }
    }

    @Override
    public String toString() {
        return "Session " + JsonService.toString(this);
    }

    public static final BusinessConnectionSession DEFAULT = new BusinessConnectionSession() {
        @Override
        public CacheMap<String, CompileStream<?>> getLocalCommandCache() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable Database getDatabase() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setDatabase(Database database) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Parameter setParameter(String name, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Parameter getParameter(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Channel channel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }

        @Override
        public void close(CloseMessage closeMessage) {
        }

        @Override
        public void close(org.hashdb.ms.net.client.CloseMessage closeMessage) {
        }

        @Override
        public void startInactive() {
        }

        @Override
        public void stopInactive() {
        }
    };
}
