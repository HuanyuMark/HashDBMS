package org.hashdb.ms.net.service;

import org.hashdb.ms.net.msg.MessageType;
import org.hashdb.ms.net.msg.ServiceMessage;

/**
 * Date: 2023/12/7 16:20
 *
 * @author huanyuMake-pecdle
 */
public class ConnectMasterMessage extends ServiceMessage {
    @Override
    public MessageType getType() {
        return MessageType.CONNECT_MASTER;
    }
}
