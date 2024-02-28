package org.hashdb.ms.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.net.nio.ClientChannelInitializer;
import org.hashdb.ms.net.nio.NettyServer;
import org.hashdb.ms.net.nio.SessionMountedHandler;
import org.hashdb.ms.support.CommandCacheConfig;
import org.jetbrains.annotations.Nullable;
import org.slf4j.event.Level;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.function.Supplier;

/**
 * Date: 2023/11/21 12:17
 *
 * @author Huanyu Mark
 */
@Slf4j
@Getter
@ConfigurationProperties(value = "server", ignoreInvalidFields = true)
public class DBServerConfig {
    private final int port;

    private final int maxConnections;

    private final long heartbeatInterval;

    private final int timeoutRetry;

    private final Level logLevel;

    private final CommandCacheConfig commandCache;

    private final int inactiveTimeout;

    private final RunMode runMode = RunMode.PRODUCTION;

    public DBServerConfig(
            Integer port,
            Integer maxConnections,
            Long heartbeatInterval,
            Integer timeoutRetry,
            Level logLevel,
            Integer inactiveTimeout,
            CommandCacheConfig commandCache
    ) {
        this.port = port == null ? 5090 : port;
        this.maxConnections = maxConnections == null ? 1_0000 : maxConnections;
        this.heartbeatInterval = heartbeatInterval == null ? 10_000 : heartbeatInterval;
        this.timeoutRetry = timeoutRetry == null ? 3 : timeoutRetry;
        this.inactiveTimeout = inactiveTimeout == null ? 10_000 : inactiveTimeout;
        this.logLevel = logLevel == null ? Level.INFO : logLevel;
        this.commandCache = commandCache == null ? new CommandCacheConfig(null, null) : commandCache;
        RunMode.config = this;
    }

    public enum RunMode {
        PRODUCTION,
        DEVELOPMENT;

        private static DBServerConfig config;

        public void run(Runnable runnable) {
            if (config.runMode == this) {
                runnable.run();
            }
        }

        public <T> @Nullable T get(Supplier<T> supplier) {
            if (config.runMode == this) {
                return supplier.get();
            }
            return null;
        }
    }

    @Bean
    @ConditionalOnClass(NettyServer.class)
    public SessionMountedHandler sessionFactoryHandler() {
        return new SessionMountedHandler();
    }

    @Bean
    @ConditionalOnClass(NettyServer.class)
    public ClientChannelInitializer clientChannelInitializer(SessionMountedHandler sessionMountedHandler) {
        return new ClientChannelInitializer(sessionMountedHandler);
    }
}
