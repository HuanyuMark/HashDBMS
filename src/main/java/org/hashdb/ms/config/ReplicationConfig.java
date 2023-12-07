package org.hashdb.ms.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.constant.ServerIdentity;
import org.hashdb.ms.net.ServerNode;

import java.util.LinkedList;
import java.util.List;

/**
 * Date: 2023/12/5 15:43
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Data
@Slf4j // 生成日志
public class ReplicationConfig {

    private ServerIdentity identity = ServerIdentity.MASTER;

    private ServerNode master;

    private List<ServerNode> slaves = new LinkedList<>();

    /**
     * sibling 本机的兄弟结
     */
    private List<ServerNode> siblings = new LinkedList<>();

    public void setIdentity(ServerIdentity identity) {
        this.identity = identity;
    }


    public void setSlaves(List<ServerNode> slaves) {
        this.slaves = slaves;
    }

    public void setMaster(ServerNode master) {
        this.master = master;
        log.info("master: {}", master);
    }
}
