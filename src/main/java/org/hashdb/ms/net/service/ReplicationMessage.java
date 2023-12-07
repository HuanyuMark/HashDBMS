package org.hashdb.ms.net.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.net.msg.Message;
import org.hashdb.ms.net.msg.MessageType;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReplicationMessage extends Message {
    private boolean isMaster = false;


    @Override
    public MessageType getType() {
        return MessageType.REPLICATION;
    }
}
