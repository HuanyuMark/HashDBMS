package org.hashdb.ms.net.bio.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.net.bio.msg.Message;
import org.hashdb.ms.net.bio.msg.MessageType;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReplicationMessage extends Message {
    private boolean isMaster = false;


    @Override
    public MessageType getType() {
        return MessageType.REPLICATION;
    }
}
