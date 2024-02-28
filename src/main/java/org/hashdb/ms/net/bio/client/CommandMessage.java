package org.hashdb.ms.net.bio.client;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.net.bio.msg.Message;
import org.hashdb.ms.net.bio.msg.MessageType;

/**
 * Date: 2023/12/1 19:57
 *
 * @author Huanyu Mark
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
