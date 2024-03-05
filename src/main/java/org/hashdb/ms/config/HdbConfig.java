package org.hashdb.ms.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.support.Checker;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Date: 2023/11/21 12:26
 *
 * @author Huanyu Mark
 */
@Slf4j
@Getter
@ConfigurationProperties("db.file")
public class HdbConfig extends PersistentConfig {
    @Deprecated
    protected final long chunkSize;

    /**
     * 如果在saveInterval 时间内, 有{@link #operationCount} 次写操作, 则将数据写入磁盘
     */
    protected final long saveInterval;
    /**
     * 如果在 {@link  #saveInterval} 时间内, 有 operationCount 次写操作, 则将数据写入磁盘
     */
    protected final int operationCount;
    
    private final String systemInfoFileName = "sys.info";

    @Deprecated
    private final String replicationConfigFileName = "replication.yml";

    /**
     * true:
     * 在第一次访问数据库时, 才从磁盘中加载数据入内存
     * false:
     * 在启动数据库服务器时, 就将所有的数据从磁盘读入内存
     */
    private final boolean lazyLoad;

    public HdbConfig(String path, Long chunkSize, Long saveInterval, Integer operationCount, Boolean lazyLoad) {
        super(path);
        this.lazyLoad = Checker.require(lazyLoad, true);
        this.chunkSize = Checker.notNegativeOrZero(chunkSize, 1024 * 1024L, STR."illegal value '\{chunkSize}' of option 'db.file.chunkSize'");
        this.saveInterval = Checker.notNegativeOrZero(saveInterval, 900L, STR."illegal value '\{saveInterval}' of option 'db.file.saveInterval'");
        this.operationCount = Checker.notNegativeOrZero(operationCount, 1, STR."illegal value '\{operationCount}' of option 'db.file.operationCount'");
    }
}
