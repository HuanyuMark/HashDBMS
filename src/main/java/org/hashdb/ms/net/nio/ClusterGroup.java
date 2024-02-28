package org.hashdb.ms.net.nio;

import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.DefaultConfig;
import org.hashdb.ms.constant.ServerIdentity;
import org.hashdb.ms.support.Checker;
import org.hashdb.ms.support.ConfigSource;
import org.hashdb.ms.support.ConfigSource.Block;
import org.hashdb.ms.support.ConfigSource.Mark;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Objects;


/**
 * Date: 2023/12/5 15:43
 *
 * @author Huanyu Mark
 */
@Slf4j
@Block(Mark.CLUSTER)
@ConfigurationProperties(value = "cluster", ignoreInvalidFields = true)
public class ClusterGroup {
    @Getter
    private ServerIdentity identity = ServerIdentity.MASTER;

    /**
     * 主结点
     */
    private ServerNode master;


    /**
     * 集群:
     * 但该节点为主结点{@link ServerIdentity#MASTER}时, 有从节点加入, 加入这个集合
     * 然后通知所有子节点(严格通知)
     */
    private ServerNodeSet slaves;

    /**
     * 集群:
     * {@link #master} 通知该结点, 有新节点加入时, 加入这个集合
     * 哨兵:
     * 其它哨兵结点
     * <p>
     * sibling 本机的兄弟结点
     */
    private ServerNodeSet siblings;

    private final Long recoverCheckInterval;

    @Resource
    private DefaultConfig defaultConfig;

    @Resource
    private ConfigSource configSource;

    public ClusterGroup(List<ServerNode> slaves, List<ServerNode> siblings, Long recoverCheckInterval) {
        if (slaves != null) {
            this.slaves = new ServerNodeSet() {
                @Override
                protected void onChange() {
                    updateConfig();
                }
            };
            this.slaves.add(slaves);
        }
        if (siblings != null) {
            this.siblings = new ServerNodeSet() {
                @Override
                protected void onChange() {
                    updateConfig();
                }
            };
            this.siblings.add(siblings);
        }
        this.recoverCheckInterval = Checker.notNegativeOrZeroNullable(recoverCheckInterval, STR."illegal value '\{recoverCheckInterval}' of option 'cluster.recover-check-interval'");
    }

    public boolean isMaster() {
        return this.identity == ServerIdentity.MASTER;
    }

    public boolean isSlave() {
        return this.identity == ServerIdentity.SLAVE;
    }

    public boolean isSentinel() {
        return this.identity == ServerIdentity.SENTINEL;
    }

    public void setIdentity(ServerIdentity identity) {
        this.identity = identity;
        updateConfig();
    }

    public ServerNodeSet slaves() {
        if (this.slaves == null) {
            this.slaves = new ServerNodeSet() {
                @Override
                protected void onChange() {
                    updateConfig();
                }
            };
        }
        return this.slaves;
    }

    public ServerNodeSet siblings() {
        if (this.siblings == null) {
            this.siblings = new ServerNodeSet() {
                @Override
                protected void onChange() {
                    updateConfig();
                }
            };
        }
        return this.siblings;
    }

    public List<ServerNode> getSlaves() {
        return slaves.all().stream().toList();
    }

    public List<ServerNode> getSiblings() {
        return siblings.all().stream().toList();
    }

    public void setMaster(ServerNode master) {
        this.master = master;
        log.info("master shift to: {}", master);
    }

    private void updateConfig() {
        configSource.updateAnnotated(this);
    }

    public long getRecoverCheckInterval() {
        return Objects.requireNonNullElseGet(recoverCheckInterval, () -> defaultConfig.getRecoverCheckInterval());
    }
}


