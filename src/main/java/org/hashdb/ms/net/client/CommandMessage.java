package org.hashdb.ms.net.client;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hashdb.ms.net.msg.Message;
import org.hashdb.ms.net.msg.MessageType;

import java.util.UUID;

/**
 * Date: 2023/12/1 19:57
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
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
