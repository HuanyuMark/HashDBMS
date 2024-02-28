package org.hashdb.ms.net.bio.service;

import org.hashdb.ms.net.bio.msg.MessageType;
import org.hashdb.ms.net.bio.msg.ServiceMessage;

/**
 * Date: 2023/12/7 16:20
 *
 * @author Huanyu Mark
 */
public class ConnectMasterMessage extends ServiceMessage {
    @Override
    public MessageType getType() {
        return MessageType.CONNECT_MASTER;
    }
}
