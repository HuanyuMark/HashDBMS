package org.hashdb.ms.config;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.net.nio.ServerNode;
import org.hashdb.ms.net.nio.ServerNodeSet;
import org.hashdb.ms.support.ConfigSource.Block;
import org.hashdb.ms.support.ConfigSource.Mark;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Date: 2024/2/20 0:24
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
@Block(Mark.SENTINEL)
@ConfigurationProperties(value = "replication", ignoreInvalidFields = true)
public class ReplicationGroupConfig {
    private ServerNode master;

    private ServerNodeSet slaves;

    public ReplicationGroupConfig(ServerNode master) {
        this.master = master;
    }
}
