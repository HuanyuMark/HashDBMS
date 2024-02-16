package org.hashdb.ms.config;

import lombok.Getter;
import org.hashdb.ms.aspect.methodAccess.ConfigLoadOnly;

/**
 * Date: 2024/1/15 14:30
 *
 * @author huanyuMake-pecdle
 */
@Getter
public class CommandCacheConfig {
    /**
     * 单位ms, 命令的缓存时间
     */
    private long aliveDuration = 30 * 60_000;

    private int cacheSize = 1000;

    @ConfigLoadOnly
    public void setAliveDuration(long aliveDuration) {
        this.aliveDuration = aliveDuration;
    }

    @ConfigLoadOnly
    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }
}
