package org.hashdb.ms.net.nio.msg.v1;

/**
 * Date: 2024/2/1 18:45
 *
 * @author Huanyu Mark
 */
public class ServerPongMessage extends ActMessage<String> implements FlyweightMessage {

    public static final ServerPongMessage DEFAULT = new ServerPongMessage(0, 0);

    public ServerPongMessage(int id, int actId) {
        super(id, actId, "PONG");
    }

    public ServerPongMessage(int id, Message<?> request) {
        super(id, request, "PONG");
    }

    public ServerPongMessage(int actId) {
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
