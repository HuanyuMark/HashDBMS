package org.hashdb.ms.net.nio;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.support.ConfigSource;
import org.hashdb.ms.support.ConfigSource.Block;
import org.hashdb.ms.support.ConfigSource.Mark;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Date: 2024/2/20 0:24
 *
 * @author Huanyu Mark
 */
@Slf4j
@Getter
@Block(Mark.SENTINEL)
@ConfigurationProperties(value = "replication", ignoreInvalidFields = true)
public class ReplicationGroup {
    private ServerNode master;

    private final ServerNodeSet<ReplicationServerNode> slaves = new ServerNodeSet<>();

    @Autowired
    private ConfigSource configSource;

    public ReplicationGroup(ServerNode master) {
        this.master = master;
    }

    public void setMaster(ServerNode master) {
        this.master = master;
        updateConfig();
    }

    private void updateConfig() {
        configSource.updateAnnotated(this);
    }

    public void sync(String writeCommand) {

    }
}
