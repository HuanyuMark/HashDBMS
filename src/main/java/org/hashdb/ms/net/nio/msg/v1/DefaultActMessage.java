package org.hashdb.ms.net.nio.msg.v1;

/**
 * Date: 2024/1/17 14:08
 *
 * @author Huanyu Mark
 */
public class DefaultActMessage extends ActMessage<String> {
    public DefaultActMessage(int id, int actId, String body) {
        super(id, actId, body);
    }

    public DefaultActMessage(int id, String body) {
        super(id, body);
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.ACT;
    }
}
