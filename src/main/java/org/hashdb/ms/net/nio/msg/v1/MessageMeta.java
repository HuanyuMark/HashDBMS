package org.hashdb.ms.net.nio.msg.v1;

import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.net.exception.IllegalMessageException;
import org.hashdb.ms.net.nio.MetaEnum;
import org.hashdb.ms.net.nio.SessionMeta;
import org.hashdb.ms.net.nio.protocol.BodyParser;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Date: 2024/1/16 21:35
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
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
    PING(BodyParser.NULL, false, (id, buf) -> new PingMessage(id)),
    /**
     * 心跳，收
     */
    PONG(BodyParser.NULL, true, (id, buf) -> new PongMessage(id, buf.readLong())),
    PROTOCOL_SWITCHING(ProtocolSwitchingMessage.class),
    RECONNECT(ReconnectMessage.class),

    SESSION_STATE(SessionStateMessage.class),
    /**
     * 切换会话的类型
     */
    SESSION_SWITCHING(BodyParser.SESSION_META, false,
            (id, buf) -> new SessionSwitchingMessage(((SessionMeta) BodyParser.SESSION_META.decode(SessionMeta.class, buf)))
    );

    private final MessageFactory factory;

    private final BodyParser bodyParser;

    private final boolean act;

    private static final MessageMeta[] ENUM_MAP = values();

    MessageMeta(Class<? extends Message<?>> messageClass) {
        this(messageClass, BodyParser.JSON);
    }

    MessageMeta(Class<? extends Message<?>> messageClass, BodyParser bodyParser) {
        this(bodyParser, new MessageConstructorFactory(messageClass, selectConstructor(messageClass), messageConstructor -> messageConstructor.getParameterTypes()[messageConstructor.getParameterCount() - 1], bodyParser));
    }

    MessageMeta(Class<? extends Message<?>> messageClass,
                BodyParser bodyParser,
                Constructor<? extends Message<?>> messageConstructor,
                Function<Constructor<? extends Message<?>>, Class<?>> messageBodyClassFinder) {
        this(bodyParser, new MessageConstructorFactory(messageClass, messageConstructor, messageBodyClassFinder, bodyParser));
    }

    MessageMeta(BodyParser bodyParser, MessageConstructorFactory factory) {
        this.bodyParser = bodyParser;
        act = factory.isAct();
        this.factory = factory;
    }

    MessageMeta(BodyParser bodyParser, boolean isActMessage, MessageFactory factory) {
        this.bodyParser = bodyParser;
        act = isActMessage;
        this.factory = factory;
    }

    private interface MessageFactory {
        Message<?> create(long id, ByteBuf buf);
    }

    private static class MessageConstructorFactory implements MessageFactory {
        private final Constructor<? extends Message<?>> messageConstructor;

        private final Class<?> messageBodyClass;

        private final BodyParser bodyParser;

        private final boolean isAct;

        public MessageConstructorFactory(Class<? extends Message<?>> messageClass, Constructor<? extends Message<?>> messageConstructor, Class<?> messageBodyClass, BodyParser bodyParser) {
            this.messageConstructor = messageConstructor;
            this.messageBodyClass = messageBodyClass;
            this.isAct = messageClass.isAssignableFrom(ActMessage.class);
            this.bodyParser = bodyParser;
        }

        public MessageConstructorFactory(Class<? extends Message<?>> messageClass, Constructor<? extends Message<?>> messageConstructor,
                                         Function<Constructor<? extends Message<?>>, Class<?>> messageBodyFinder, BodyParser bodyParser) {
            this(messageClass, messageConstructor, messageBodyFinder.apply(messageConstructor), bodyParser);
        }

        public boolean isAct() {
            return isAct;
        }

        @Override
        public Message<?> create(long id, ByteBuf buf) {
            try {
                if (isAct) {
                    var actId = buf.readLong();
                    var body = bodyParser.decode(messageBodyClass, buf);
                    return messageConstructor.newInstance(id, actId, body);
                }
                var body = bodyParser.decode(messageBodyClass, buf);
                return messageConstructor.newInstance(id, body);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                log.error("create message error", e);
                System.exit(1);
                throw new DBSystemException(e);
            }
        }
    }

    private static Constructor<? extends Message<?>> selectConstructor(Class<? extends Message<?>> messageClass) {
        boolean isAct = messageClass.isAssignableFrom(ActMessage.class);
        @SuppressWarnings("unchecked")
        var constructors = (((Constructor<? extends Message<?>>[]) messageClass.getConstructors()));
        Predicate<Constructor<? extends Message<?>>> filter;
        if (isAct) {
            filter = c -> {
                if (c.getParameterCount() != 3) {
                    return false;
                }
                Class<?>[] parameterTypes = c.getParameterTypes();
                return (parameterTypes[0] == long.class || parameterTypes[0] == Long.class) &&
                        (parameterTypes[1] == long.class || parameterTypes[1] == Long.class);
            };
        } else {
            filter = c -> {
                if (c.getParameterCount() != 2) {
                    return false;
                }
                Class<?>[] parameterTypes = c.getParameterTypes();
                return parameterTypes[0] == long.class || parameterTypes[0] == Long.class;
            };
        }
        try {
            Constructor<? extends Message<?>> messageConstructor = Arrays.stream(constructors).filter(filter).findFirst().orElseThrow(() -> {
                if (isAct) {
                    return new NoSuchMethodException("class '" + messageClass + "' definition is illegal. it should contain a constructor with parameters " +
                            "'[" + long.class + "," + long.class + "," + Object.class + "]'");
                }
                return new NoSuchMethodException("class '" + messageClass + "' definition is illegal. it should contain a constructor with parameters " +
                        "'[" + long.class + "," + Object.class + "]'");
            });
            messageConstructor.setAccessible(true);
            return messageConstructor;
        } catch (NoSuchMethodException e) {
            error(e.getMessage(), e);
            System.exit(1);
            throw new DBSystemException(e);
        }
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
    public Message<?> create(long id, ByteBuf buf) {
        return factory.create(id, buf);
    }

    public boolean isActMessage() {
        return act;
    }

    public BodyParser bodyParser() {
        return bodyParser;
    }

    @Override
    public int key() {
        return ordinal();
    }
}
