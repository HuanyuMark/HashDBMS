package org.hashdb.ms.net.nio.msg.v1;

/**
 * Date: 2024/1/17 13:56
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ReconnectMessage extends Message<Long> {
    public ReconnectMessage(long id, Long body) {
        super(id, body);
    }

    public ReconnectMessage(Long body) {
        super(body);
    }

    @Override
    public MessageType type() {
        return MessageType.RECONNECT;
    }
}
