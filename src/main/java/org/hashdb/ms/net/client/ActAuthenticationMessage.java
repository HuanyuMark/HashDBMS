package org.hashdb.ms.net.client;

import org.hashdb.ms.net.msg.Message;
import org.hashdb.ms.net.msg.MessageType;

/**
 * Date: 2023/12/1 3:08
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ActAuthenticationMessage extends Message {
    @Override
    public MessageType getType() {
        return MessageType.ACT_AUTH;
    }
}
