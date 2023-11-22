package org.hashdb.ms.config;

import lombok.Getter;
import org.hashdb.ms.aspect.methodAccess.DisposableUse;
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
    private Integer port;

    @DisposableUse
    public void setPort(Integer port) {
        this.port = port;
    }
}
