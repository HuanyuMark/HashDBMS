package org.hashdb.ms.net.nio.msg.v1;

/**
 * Date: 2024/1/17 13:56
 *
 * @author Huanyu Mark
 */
public class ReconnectMessage extends Message<Integer> {
    public ReconnectMessage(int id, Integer body) {
        super(id, body);
    }

    public ReconnectMessage(Integer body) {
        super(body);
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.RECONNECT;
    }
}
