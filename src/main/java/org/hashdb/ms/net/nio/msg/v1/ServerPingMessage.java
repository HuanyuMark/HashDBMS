package org.hashdb.ms.net.nio.msg.v1;

/**
 * Date: 2024/2/1 18:42
 *
 * @author huanyuMake-pecdle
 */
public class ServerPingMessage extends Message<String> implements FlyweightMessage {

    public static final ServerPingMessage DEFAULT = new ServerPingMessage(0L);

    public ServerPingMessage(long id) {
        super(id, "PING");
    }

    public ServerPingMessage() {
        super("PING");
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.PING;
    }

    private ServerPongMessage pong;

    public ServerPongMessage pong() {
        return pong == null ? (pong = new ServerPongMessage(id)) : pong;
    }

//    @Override
//    public ByteBuf toByteBuf(Protocol protocol) {
//        if (protocol != ServerPingMessage.DEFAULT_BYTEBUF_PROTOCOL) {
//            DEFAULT_BYTEBUF = protocol.codec().encode(ByteBufAllocator.DEFAULT.buffer(), this);
//            ServerPingMessage.DEFAULT_BYTEBUF_PROTOCOL = protocol;
//        }
//        return DEFAULT_BYTEBUF;
//    }
}
