package org.hashdb.ms.net.client;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.net.msg.Message;
import org.hashdb.ms.net.msg.MessageType;

import java.util.UUID;

/**
 * Date: 2023/12/1 13:41
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CloseMessage extends Message {
    @Override
    public MessageType getType() {
        return MessageType.CLOSE;
    }
}
