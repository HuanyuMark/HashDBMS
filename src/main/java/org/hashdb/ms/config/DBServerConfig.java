package org.hashdb.ms.config;

import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.aspect.methodAccess.ConfigLoadOnly;
import org.hashdb.ms.net.nio.ClientChannelInitializer;
import org.hashdb.ms.net.nio.NettyServer;
import org.hashdb.ms.net.nio.SessionMountedHandler;
import org.hashdb.ms.net.nio.protocol.CodecDispatcher;
import org.slf4j.event.Level;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Date: 2023/11/21 12:17
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
@Getter
@Configuration
@ConfigurationProperties("server")
@EnableConfigurationProperties
public class DBServerConfig {
    private int port = 5090;

    private int maxConnections = 1_0000;

    private long heartbeatInterval = 10_000;

    private int timeoutRetry = 3;

    private Level logLevel = Level.INFO;

    private CommandCacheConfig commandCache = new CommandCacheConfig();

    private int inactiveTimeout = 10_000;

    @Bean
    @ConditionalOnClass(NettyServer.class)
    public LoggingHandler loggingHandler() {
        return new LoggingHandler(LogLevel.INFO);
    }

    @Bean
    @ConditionalOnClass(NettyServer.class)
    public SessionMountedHandler sessionFactoryHandler() {
        return new SessionMountedHandler();
    }

    @Bean
    @ConditionalOnClass(NettyServer.class)
    public CodecDispatcher hashProtocolCodecHandler() {
        return new CodecDispatcher();
    }

    @Bean
    @ConditionalOnClass(NettyServer.class)
    public ClientChannelInitializer clientChannelInitializer(
            CodecDispatcher messageCodec,
            SessionMountedHandler sessionMountedHandler,
            LoggingHandler loggingHandler
    ) {
        return new ClientChannelInitializer(messageCodec, sessionMountedHandler, loggingHandler);
    }

    public void setPort(int port) {
        this.port = port;
    }

    @ConfigLoadOnly
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    @ConfigLoadOnly
    public void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    @ConfigLoadOnly
    public void setTimeoutRetry(int timeoutRetry) {
        this.timeoutRetry = timeoutRetry;
    }

    @ConfigLoadOnly
    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
    }

    @ConfigLoadOnly
    public void setCommandCache(CommandCacheConfig commandCache) {
        this.commandCache = commandCache;
    }

    public void setInactiveTimeout(int inactiveTimeout) {
        this.inactiveTimeout = inactiveTimeout;
    }
}
