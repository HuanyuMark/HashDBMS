package org.hashdb.ms.net.msg;

import lombok.Data;

@Data
public class ReplicationMessage extends Message{
     private boolean isMaster = false;




    @Override
    public MessageType getType() {
        return MessageType.REPLICATION;
    }
}
