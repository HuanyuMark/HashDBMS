package org.hashdb.ms.net.client;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.net.msg.Message;
import org.hashdb.ms.net.msg.MessageType;

/**
 * Date: 2023/12/1 19:57
 *
 * @author huanyuMake-pecdle
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommandMessage extends Message {

    private String command;

    @Override
    public MessageType getType() {
        return MessageType.COMMAND;
    }
}
