package org.hashdb.ms.net.nio.msg.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hashdb.ms.net.nio.NettyConnectionSession;
import org.hashdb.ms.net.nio.protocol.BodyParser;
import org.hashdb.ms.util.JsonService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Date: 2024/1/16 21:20
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class Message<B> {
    private static final AtomicLong messageIdAccumulator = new AtomicLong(0);

    protected final long id;

    private NettyConnectionSession session;

    private byte[] bodyBytes;

    public abstract MessageType type();

    public NettyConnectionSession session() {
        return session;
    }

    public void session(NettyConnectionSession session) {
        this.session = session;
    }


    /**
     * @param id   由协议层调用,实例化Message
     * @param body
     */
    public Message(long id, B body) {
        this.id = id;
        this.body = body;
    }

    public Message(B body) {
        id = messageIdAccumulator.incrementAndGet();
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

    /**
     * 规定该body的解析方式
     */
    public @NotNull BodyParser bodyParser() {
        return BodyParser.JSON;
    }

    @Override
    public String toString() {
        return JsonService.toString(this);
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public long getSessionId() {
        return session == null ? -1 : session.id();
    }
}
