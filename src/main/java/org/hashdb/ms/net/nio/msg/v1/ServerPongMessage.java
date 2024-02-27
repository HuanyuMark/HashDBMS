package org.hashdb.ms.net.nio.msg.v1;

/**
 * Date: 2024/2/1 18:45
 *
 * @author huanyuMake-pecdle
 */
public class ServerPongMessage extends ActMessage<String> implements FlyweightMessage {

    public static final ServerPongMessage DEFAULT = new ServerPongMessage(0L, 0L);

    public ServerPongMessage(long id, long actId) {
        super(id, actId, "PONG");
    }

    public ServerPongMessage(long id, Message<?> request) {
        super(id, request, "PONG");
    }

    public ServerPongMessage(long actId) {
        super(actId, "PONG");
    }

    public ServerPongMessage(Message<?> request) {
        super(request, "PONG");
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.PONG;
    }
}
