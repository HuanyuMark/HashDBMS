package org.hashdb.ms.net.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.net.msg.MessageType;
import org.hashdb.ms.net.msg.ServiceMessage;

/**
 * Date: 2023/12/1 2:13
 *
 * @author huanyuMake-pecdle
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ActAuthenticationMessage extends ServiceMessage {

    /**
     * 现在不需要验证, 所以默认成功
     */
    private boolean success;

    private final String app = "hashDBMS";

    private String user;

    @Override
    public MessageType getType() {
        return MessageType.ACT_AUTH;
    }

    public ActAuthenticationMessage(boolean success) {
        this.success = success;
    }

    public ActAuthenticationMessage() {
        this.success = true;
    }
}
