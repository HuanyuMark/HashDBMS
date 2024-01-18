package org.hashdb.ms.net.nio.msg.v1;

/**
 * Date: 2024/1/18 21:54
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ActAppCommandMessage extends ActMessage<Object> {
    public ActAppCommandMessage(long id, long actId, Object body) {
        super(id, actId, body);
    }

    public ActAppCommandMessage(long actId, Object body) {
        super(actId, body);
    }

    @Override
    public MessageType type() {
        return MessageType.ACT_APP_COMMAND;
    }
}
