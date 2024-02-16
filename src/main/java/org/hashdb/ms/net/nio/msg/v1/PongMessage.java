package org.hashdb.ms.net.nio.msg.v1;

/**
 * Date: 2024/2/1 18:45
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class PongMessage extends ActMessage<String> {
    public PongMessage(long id, long actId) {
        super(id, actId, "PONG");
    }

    public PongMessage(long id, Message<?> request) {
        super(id, request, "PONG");
    }

    public PongMessage(long actId) {
        super(actId, "PONG");
    }

    public PongMessage(Message<?> request) {
        super(request, "PONG");
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.PONG;
    }
}
