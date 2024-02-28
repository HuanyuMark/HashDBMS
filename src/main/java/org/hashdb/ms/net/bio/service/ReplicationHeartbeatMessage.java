package org.hashdb.ms.net.bio.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.net.bio.msg.MessageType;
import org.hashdb.ms.net.bio.msg.ServiceMessage;

/**
 * Date: 2023/12/7 16:17
 * 主从之间 Heartbeat 的消息, 互相间确认 offset, 看看是否所有的写
 * 命令已经传播完成
 *
 * @author Huanyu Mark
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReplicationHeartbeatMessage extends ServiceMessage {

    private long offset;

    @Override
    public MessageType getType() {
        return MessageType.REPL_HEARTBEAT;
    }
}
