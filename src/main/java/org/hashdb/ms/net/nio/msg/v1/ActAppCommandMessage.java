package org.hashdb.ms.net.nio.msg.v1;

/**
 * Date: 2024/1/18 21:54
 *
 * @author huanyuMake-pecdle
 */
public class ActAppCommandMessage extends ActMessage<Object> {
    public ActAppCommandMessage(long id, long actId, Object body) {
        super(id, actId, body);
    }

    public ActAppCommandMessage(long actId, Object body) {
        super(actId, body);
    }

    public ActAppCommandMessage(Message<?> request, Object body) {
        super(request, body);
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.ACT_APP_COMMAND;
    }
}
