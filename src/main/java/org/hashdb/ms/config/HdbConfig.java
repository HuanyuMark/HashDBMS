package org.hashdb.ms.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.aspect.methodAccess.ConfigLoadOnly;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Date: 2023/11/21 12:26
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
@Getter
@Configuration
@ConfigurationProperties("db.file")
@EnableConfigurationProperties
public class HdbConfig extends PersistentConfig {
    private final String systemInfoFileName = "sys.info";
    private final String replicationConfigFileName = "replication.yml";

    /**
     * true:
     * 在第一次访问数据库时, 才从磁盘中加载数据入内存
     * false:
     * 在启动数据库服务器时, 就将说有的数据从磁盘读入内存
     */
    private boolean lazyLoad = true;

    private String replicationConfigPath = System.getProperty("user.dir");


    @ConfigLoadOnly
    public void setLazyLoad(boolean lazyLoad) {
        this.lazyLoad = lazyLoad;
    }

    @ConfigLoadOnly
    public void setReplicationConfigPath(String replicationConfigPath) {
        this.replicationConfigPath = replicationConfigPath;
    }
}
