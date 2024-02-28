package org.hashdb.ms.net.bio.client;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.net.bio.msg.Message;
import org.hashdb.ms.net.bio.msg.MessageType;

/**
 * Date: 2023/12/1 3:11
 *
 * @author Huanyu Mark
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DisconnectMessage extends Message {
    @Override
    public MessageType getType() {
        return null;
    }
}
