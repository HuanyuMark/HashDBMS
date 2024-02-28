package org.hashdb.ms.net.nio;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Resource;
import lombok.Getter;
import org.hashdb.ms.config.DefaultConfig;
import org.hashdb.ms.support.Checker;
import org.hashdb.ms.support.ConfigSource;
import org.hashdb.ms.support.ConfigSource.Block;
import org.hashdb.ms.support.ConfigSource.Mark;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Objects;

/**
 * Date: 2024/2/21 18:57
 *
 * @author Huanyu Mark
 * @version 0.0.1
 */
@Block(Mark.SENTINEL)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ConfigurationProperties(value = "sentinel", ignoreInvalidFields = true)
public class SentinelGroup {
    /**
     * sentinel集群的其它结点
     */
    @Getter
    private final ServerNodeSet<?> sentinels;

    /**
     * 监控的主结点
     */
    @Getter
    private ServerNode master;

    /**
     * 监控的从节点
     */
    @Getter
    private final ServerNodeSet<ReplicationServerNode> slaves;

    private final Long recoverCheckInterval;
    /**
     * 每次更改所有与主从结点拓扑图时, 都会递增offset
     * 在该sentinel故障恢复后, 可以根据offset, 确认集群
     * 信息是否变动,如果变动, 则从offset最高的sentinel
     * 中获取最新拓扑图, 更新该sentinel的集群信息
     */
    @Getter
    private int offset;

    private boolean init = true;

    @Resource
    private DefaultConfig defaultConfig;

    @Resource
    private ConfigSource configSource;

    public SentinelGroup(
            List<ServerNode> sentinels,
            ServerNode master,
            List<ReplicationServerNode> slaves,
            Long recoverCheckInterval,
            int offset
    ) {
        Checker.notNegativeOrZero(recoverCheckInterval, 5_000, STR."illegal value \{recoverCheckInterval} of option 'sentinel.recover-check-interval'");
        this.sentinels = newSet(sentinels);
        this.master = master;
        this.slaves = newSet(slaves);
        this.recoverCheckInterval = Checker.notNegativeOrZeroNullable(recoverCheckInterval, STR."illegal value '\{recoverCheckInterval}' of option 'cluster.recover-check-interval'");
        this.offset = offset;
        this.init = false;
    }

    public long getRecoverCheckInterval() {
        return Objects.requireNonNullElseGet(recoverCheckInterval, () -> defaultConfig.getRecoverCheckInterval());
    }

    public void setMaster(ServerNode master) {
        this.master = master;
        ++offset;
        updateConfig();
    }

    @NotNull
    private <N extends ServerNode> ServerNodeSet<N> newSet(List<N> sentinels) {
        return new ServerNodeSet<>(sentinels) {
            @Override
            protected void onChange() {
                if (init) {
                    return;
                }
                ++SentinelGroup.this.offset;
                updateConfig();
            }
        };
    }

    protected void updateConfig() {
        configSource.updateAnnotated(this);
    }
}
