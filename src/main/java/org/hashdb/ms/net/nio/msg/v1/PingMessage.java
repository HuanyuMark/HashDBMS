package org.hashdb.ms.net.nio.msg.v1;

/**
 * Date: 2024/2/1 18:42
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class PingMessage extends Message<String> {
    public PingMessage(long id) {
        super(id, "PING");
    }

    public PingMessage() {
        super("PING");
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.PING;
    }
}
