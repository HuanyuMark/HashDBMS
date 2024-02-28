package org.hashdb.ms.net.bio.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.net.bio.client.CommandMessage;
import org.hashdb.ms.net.bio.msg.MessageType;
import org.hashdb.ms.net.bio.msg.ServiceMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Date: 2023/12/1 20:02
 *
 * @author Huanyu Mark
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ActCommandMessage extends ServiceMessage {

    private String command;

    public ActCommandMessage(@NotNull CommandMessage commandMessage, String result) {
        this.command = commandMessage.getCommand();
        setData(result);
    }

    @Override
    public MessageType getType() {
        return MessageType.ACK_COMMAND;
    }
}
