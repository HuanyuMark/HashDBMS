package org.hashdb.ms.net.nio.msg.v1;

import org.hashdb.ms.net.nio.TransientConnectionSession;
import org.hashdb.ms.util.JsonService;
import org.hashdb.ms.util.LongIdentityGenerator;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2024/1/16 21:20
 *
 * @author huanyuMake-pecdle
 */
public abstract class Message<B> {
    private static final LongIdentityGenerator idGenerator = new LongIdentityGenerator(0, Long.MAX_VALUE);

    protected final long id;

    private TransientConnectionSession session;

    public abstract MessageMeta getMeta();

    public TransientConnectionSession session() {
        return session;
    }

    public void session(TransientConnectionSession session) {
        if (this.session != null) {
            throw new IllegalArgumentException("session can not be set again: " + this);
        }
        this.session = session;
    }


    /**
     * @param id   由协议层{@link org.hashdb.ms.net.nio.protocol.CodecDispatcher}调用,实例化Message
     * @param body 消息体
     */
    public Message(long id, @Nullable B body) {
        if (id > 0) {
            throw new IllegalArgumentException("Constructor 'Message(long,B)' should be called in message codec. can not be call manually");
        }
        this.id = id;
        this.body = body;
    }

    public Message(@Nullable B body) {
        id = idGenerator.nextId();
        this.body = body;
    }

    @Nullable
    protected final B body;

    public long id() {
        return id;
    }

    public @Nullable B body() {
        return body;
    }

    @Override
    public String toString() {
        return JsonService.toString(this);
    }

    public long sessionId() {
        return session == null ? -1 : session.id();
    }
}
