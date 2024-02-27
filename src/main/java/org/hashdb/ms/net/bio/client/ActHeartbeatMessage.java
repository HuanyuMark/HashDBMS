package org.hashdb.ms.net.bio.client;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hashdb.ms.net.bio.msg.MessageType;
import org.hashdb.ms.net.bio.service.HeartbeatMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Date: 2023/12/1 2:55
 *
 * @author huanyuMake-pecdle
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ActHeartbeatMessage extends HeartbeatMessage {


    @Override
    public MessageType getType() {
        return MessageType.ACT_HEARTBEAT;
    }

    public boolean ack(@NotNull HeartbeatMessage heartbeatMessage) {
        return beat == heartbeatMessage.getBeat() &&
                getTimestamp() != heartbeatMessage.getTimestamp();
    }

    @Override
    public String toString() {
        return "ActHeartbeatMessage{" +
                ", beat=" + beat +
                ", id=" + id +
                ", timestamp=" + timestamp +
                ", data='" + data + '\'' +
                '}';
    }
}
