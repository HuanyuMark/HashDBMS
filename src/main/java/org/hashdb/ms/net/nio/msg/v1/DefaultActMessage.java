package org.hashdb.ms.net.nio.msg.v1;

/**
 * Date: 2024/1/17 14:08
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class DefaultActMessage extends ActMessage<String> {
    public DefaultActMessage(long id, long actId, String body) {
        super(id, actId, body);
    }

    public DefaultActMessage(long id, String body) {
        super(id, body);
    }

    @Override
    public MessageType type() {
        return MessageType.ACT;
    }
}
