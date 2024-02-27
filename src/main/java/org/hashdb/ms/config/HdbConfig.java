package org.hashdb.ms.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Date: 2023/11/21 12:26
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
@Getter
@ConfigurationProperties("db.file")
public class HdbConfig extends PersistentConfig {
    private final String systemInfoFileName = "sys.info";
    private final String replicationConfigFileName = "replication.yml";

    /**
     * true:
     * 在第一次访问数据库时, 才从磁盘中加载数据入内存
     * false:
     * 在启动数据库服务器时, 就将说有的数据从磁盘读入内存
     */
    private final boolean lazyLoad;

    @Deprecated
    private final String replicationConfigPath;

    public HdbConfig(String path, Long chunkSize, Long saveInterval, Boolean lazyLoad, String replicationConfigPath) {
        super(path, chunkSize, saveInterval);
        this.lazyLoad = lazyLoad == null || lazyLoad;
        this.replicationConfigPath = replicationConfigPath == null ? System.getProperty("user.dir") : replicationConfigPath;
    }
}
