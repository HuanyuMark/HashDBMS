package org.hashdb.ms.config;

import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import org.hashdb.ms.net.nio.ServerNode;
import org.hashdb.ms.net.nio.ServerNodeSet;
import org.hashdb.ms.support.Checker;
import org.hashdb.ms.support.ConfigSource.Block;
import org.hashdb.ms.support.ConfigSource.Mark;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Objects;

/**
 * Date: 2024/2/21 18:57
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Getter
@Block(Mark.SENTINEL)
@ConfigurationProperties(value = "sentinel", ignoreInvalidFields = true)
public class SentinelGroupConfig {
    /**
     * sentinel集群的其它结点
     */
    private final ServerNodeSet sentinels;

    /**
     * 监控的主结点
     */
    @Setter
    private ServerNode master;

    /**
     * 监控的从节点
     */
    private final ServerNodeSet slaves;

    private final Long recoverCheckInterval;
    /**
     * 每次更改所有与主从结点拓扑图时, 都会递增offset
     * 在该sentinel故障恢复后, 可以根据offset, 确认集群
     * 信息是否变动,如果变动, 则从offset最高的sentinel
     * 中获取最新拓扑图, 更新该sentinel的集群信息
     */
    private int offset;

    private boolean init = true;

    @Resource
    private DefaultConfig defaultConfig;

    public SentinelGroupConfig(
            List<ServerNode> sentinels,
            ServerNode master,
            List<ServerNode> slaves,
            Long recoverCheckInterval,
            int offset
    ) {
        Checker.notNegativeOrZero(recoverCheckInterval, 0, STR."illegal value \{recoverCheckInterval} of option 'sentinel.recover-check-interval'");
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

    @NotNull
    private ServerNodeSet newSet(List<ServerNode> sentinels) {
        return new ServerNodeSet(sentinels) {
            @Override
            protected void onChange() {
                if (init) {
                    return;
                }
                ++SentinelGroupConfig.this.offset;
            }
        };
    }
}
