package org.hashdb.ms.net.client;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.net.msg.Message;
import org.hashdb.ms.net.msg.MessageType;

/**
 * Date: 2023/12/1 13:41
 *
 * @author huanyuMake-pecdle
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CloseMessage extends Message {
    @Override
    public MessageType getType() {
        return MessageType.CLOSE;
    }
}
