package org.hashdb.ms.net.client;

import org.hashdb.ms.net.msg.MessageType;
import org.hashdb.ms.net.service.HeartbeatMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Date: 2023/12/1 2:55
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ActHeartbeatMessage extends HeartbeatMessage {

    @Override
    public MessageType getType() {
        return MessageType.ACT_HEARTBEAT;
    }

    public boolean ack(@NotNull HeartbeatMessage heartbeatMessage) {
        return heartbeatMessage.getIp().equals(ip) &&
                port == heartbeatMessage.getPort() &&
                beat == heartbeatMessage.getBeat() &&
                timestamp != heartbeatMessage.getTimestamp();
    }
}
