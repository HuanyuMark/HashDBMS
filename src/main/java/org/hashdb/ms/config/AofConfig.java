package org.hashdb.ms.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.support.Exit;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Date: 2023/12/5 16:56
 * {@link #saveInterval} 如果为-1, 则每次执行完写命令后,会将写命令直接追加入AOF中持久化
 * 大于0, 则每隔多少ms,会将缓冲区的的写命令追加入AOF中持久化
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
@Getter
@ConfigurationProperties(value = "db.aof", ignoreInvalidFields = true)
public class AofConfig extends PersistentConfig {
    /**
     * 是否开启aof持久化
     */
    private final boolean enabled;

    /**
     * 当aof文件的行数超过该值时，会触发rewrite操作
     */
    private final int rewriteSize;

    /**
     * 触发重写时, 会将此时数据库的状态以一系列写命令的
     * 形式, 写入该文件, 在下次aof重写触发前, base.aof不会改变,
     * 只有每次重写时, 才会改变该文件
     */
    protected final String aofBaseFileName = "base.aof";

    public AofConfig(String path, Long chunkSize, Long saveInterval, Boolean enabled, Integer rewriteSize) {
        super(path, chunkSize, saveInterval);
        this.enabled = enabled != null && enabled;
        if (rewriteSize == null) {
            this.rewriteSize = 1500;
        } else if (rewriteSize < 0) {
            log.error("rewriteSize must be greater than 0");
            throw Exit.exception();
        } else {
            this.rewriteSize = rewriteSize;
        }
    }
}
