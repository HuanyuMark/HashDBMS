package org.hashdb.ms.config;

import lombok.Getter;
import org.hashdb.ms.aspect.methodAccess.ConfigLoadOnly;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Date: 2023/11/21 12:17
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Getter
@Configuration
@ConfigurationProperties("server")
@EnableConfigurationProperties
public class DBServerConfig {
    private int port = 4090;

    private int maxConnections = 1_0000;

    private long heartbeatInterval = 10_000;
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
}
