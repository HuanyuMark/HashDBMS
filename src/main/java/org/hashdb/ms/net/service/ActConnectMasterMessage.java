package org.hashdb.ms.net.service;

import org.hashdb.ms.net.msg.MessageType;
import org.hashdb.ms.net.msg.ServiceMessage;

/**
 * Date: 2023/12/7 16:21
 *
 * @author huanyuMake-pecdle
 */
public class ActConnectMasterMessage extends ServiceMessage {
    @Override
    public MessageType getType() {
        return MessageType.ACT_CONNECT_MASTER;
    }
}
