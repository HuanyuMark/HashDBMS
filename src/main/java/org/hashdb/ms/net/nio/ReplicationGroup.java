package org.hashdb.ms.net.nio;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.support.Checker;
import org.hashdb.ms.support.ConfigSource;
import org.hashdb.ms.support.ConfigSource.Block;
import org.hashdb.ms.support.ConfigSource.Mark;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date: 2024/2/20 0:24
 *
 * @author Huanyu Mark
 */
@Slf4j
@Block(Mark.SENTINEL)
@ConfigurationProperties(value = "replication", ignoreInvalidFields = true)
public class ReplicationGroup {

    @Getter
    private ServerNode master;

    @Getter
    private final ServerNodeSet<ReplicationServerNode> slaves = new ServerNodeSet<>() {
        @Override
        protected void onAdd(ServerNode node) {
            if (brokerList.isEmpty()) {
                return;
            }
            var itr = brokerList.listIterator(brokerList.size());
            while (itr.hasPrevious()) {
                var broker = itr.previous();
                if (broker.isEnable()) {
                    continue;
                }
                broker.enableReplication(new ReplicationContextImpl(ReplicationGroup.this));
            }
        }
    };

    @Autowired
    private ConfigSource configSource;

    @Getter
    @JsonIgnore
    private final int replicationCacheSize;

    private Map<Database, ReplicationContext> replicationContextMap;

    private final List<ReplicationContextBroker> brokerList = new ArrayList<>();

    public ReplicationGroup(ServerNode master, int replicationCacheSize) {
        this.master = master;
        if (!slaves.all().isEmpty()) {
            replicationContextMap = new ConcurrentHashMap<>();
        }
        this.replicationCacheSize = Checker.require(replicationCacheSize, 500);
    }

    /**
     * @param master 如果为null, 则本结点晋升为主结点, 否则, 只是切换主结点
     */
    public void setMaster(ServerNode master) {
        if (master == null) {
            replicationContextMap = new ConcurrentHashMap<>();
        } else {
            replicationContextMap = null;
        }
        this.master = master;
        updateConfig();
    }

    private void updateConfig() {
        configSource.updateAnnotated(this);
    }

    @JsonIgnore
    public boolean isMaster() {
        return master == null;
    }

    @JsonIgnore
    public ReplicationContext getReplicationContext(Database database) {
        if (master == null && !slaves.all().isEmpty()) {
            return replicationContextMap.computeIfAbsent(database, db -> new ReplicationContextImpl(this));
        }
        var broker = new ReplicationContextBroker() {
            @Override
            protected void close(ReplicationContextBroker broker) {
                brokerList.remove(broker);
            }
        };
        brokerList.add(broker);
        return broker;
    }
}
