package org.hashdb.ms.support;

import lombok.Getter;
import org.springframework.context.annotation.Lazy;


/**
 * Date: 2024/1/15 14:30
 *
 * @author Huanyu Mark
 */
@Lazy
@Getter
public class CommandCacheConfig {
    /**
     * 单位ms, 命令的缓存时间
     */
    private final long aliveDuration;

    private final int cacheSize;

    public CommandCacheConfig(Long aliveDuration, Integer cacheSize) {
        this.aliveDuration = aliveDuration == null ? 30 * 60_000 : aliveDuration;
        this.cacheSize = cacheSize == null ? 1000 : cacheSize;
    }
}
