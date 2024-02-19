package org.hashdb.ms.net.nio.msg.v1;

import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.net.exception.IllegalMessageException;
import org.hashdb.ms.net.nio.MetaEnum;
import org.hashdb.ms.net.nio.SessionMeta;
import org.hashdb.ms.net.nio.protocol.BodyParser;
import org.hashdb.ms.net.nio.protocol.HashV1ProtocolCodec;
import org.slf4j.LoggerFactory;

/**
 * Date: 2024/1/16 21:35
 *
 * @author huanyuMake-pecdle
 */
// TODO: 2024/2/3 将依靠构造器构造Message的手段改成依靠工厂类来构造的手段
@Slf4j
public enum MessageMeta implements MetaEnum {
    ACT(DefaultActMessage.class),
    /**
     * 发送用户业务命令的执行结果
     */
    ACT_APP_COMMAND(ActAppCommandMessage.class),

    ACT_AUTHENTICATION(ActAuthenticationMessage.class),
    /**
     * 操作数据库,切换数据库,创建用户,登录时用的消息体
     */
    APP_COMMAND(AppCommandMessage.class),

    AUTHENTICATION(AuthenticationMessage.class),

    ERROR(ErrorMessage.class),
    /**
     * 心跳，发
     */
    PING(PingMessage.class, BodyParser.NULL, (id, buf) -> new PingMessage(id)),
    /**
     * 心跳，收
     */
    PONG(PongMessage.class, BodyParser.NULL, (id, buf) -> new PongMessage(id, buf.readLong())),
    PROTOCOL_SWITCHING(ProtocolSwitchingMessage.class),
    RECONNECT(ReconnectMessage.class),

    SESSION_STATE(SessionStateMessage.class),
    /**
     * 切换会话的类型
     */
    @Deprecated
    SESSION_SWITCHING(SessionSwitchingMessage.class, BodyParser.SESSION_META,
            (id, buf) -> new SessionSwitchingMessage(((SessionMeta) BodyParser.SESSION_META.decode(SessionMeta.class, buf)))
    ),
    SESSION_UPGRADE(SessionUpgradeMessage.class, BodyParser.SESSION_META,
            (id, buf) -> new SessionUpgradeMessage(((SessionMeta) BodyParser.SESSION_META.decode(SessionMeta.class, buf))));

    private final HashV1ProtocolCodec.Codec.MessageDecoder<?> factory;

    private final BodyParser bodyParser;

    private final boolean act;

    private static final MessageMeta[] ENUM_MAP = values();

    public static int count() {
        return ENUM_MAP.length;
    }

    MessageMeta(Class<? extends Message<?>> messageClass) {
        this(messageClass, BodyParser.JSON);
    }

    @SuppressWarnings("unchecked")
    <M extends Message<?>> MessageMeta(Class<M> messageClass, BodyParser bodyParser) {
        this(bodyParser, new HashV1ProtocolCodec.Codec.MessageConstructorDecoder<>(messageClass, messageConstructor -> messageConstructor.getParameterTypes()[messageConstructor.getParameterCount() - 1], bodyParser));
    }

    MessageMeta(BodyParser bodyParser, HashV1ProtocolCodec.Codec.MessageConstructorDecoder<?> factory) {
        this.bodyParser = bodyParser;
        act = factory.isAct();
        this.factory = factory;
    }

    <M extends Message<?>> MessageMeta(Class<M> messageClass, BodyParser bodyParser, HashV1ProtocolCodec.Codec.MessageDecoder<? extends M> factory) {
        this.bodyParser = bodyParser;
        act = ActMessage.class.isAssignableFrom(messageClass);
        this.factory = factory;
    }

    private static void error(String msg, Throwable e) {
        LoggerFactory.getLogger(MessageMeta.class).error(msg, e);
    }

    public static MessageMeta resolve(int code) throws IllegalMessageException {
        try {
            return ENUM_MAP[code];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalMessageException("illegal message type '" + code + "'");
        }
    }

    /**
     * @param id message id
     * @return message instance
     */
    @Deprecated
    public Message<?> create(long id, ByteBuf buf) {
        return factory.decode(id, buf);
    }

    @Deprecated
    public boolean isActMessage() {
        return act;
    }

    @Deprecated
    public BodyParser bodyParser() {
        return bodyParser;
    }

    @Override
    public int key() {
        return ordinal();
    }
}
