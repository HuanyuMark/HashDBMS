package org.hashdb.ms.net.nio.msg.v1;

/**
 * Date: 2024/2/1 18:42
 *
 * @author huanyuMake-pecdle
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
