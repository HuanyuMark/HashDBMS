package org.hashdb.ms.net.bio.client;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.net.bio.msg.Message;
import org.hashdb.ms.net.bio.msg.MessageType;

/**
 * Date: 2023/12/1 13:41
 *
 * @author Huanyu Mark
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CloseMessage extends Message {
    @Override
    public MessageType getType() {
        return MessageType.CLOSE;
    }
}
