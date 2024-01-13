package org.hashdb.ms.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.aspect.methodAccess.ConfigLoadOnly;
import org.slf4j.event.Level;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Date: 2023/11/21 12:17
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
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

    @Getter
    public static class CommandCacheConfig {
        /**
         * 单位ms, 命令的缓存时间
         */
        long aliveDuration = 30 * 60_000;

        int cacheSize = 1000;

        @ConfigLoadOnly
        public void setAliveDuration(long aliveDuration) {
            this.aliveDuration = aliveDuration;
        }
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
}
