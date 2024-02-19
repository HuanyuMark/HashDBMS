package org.hashdb.ms.net.nio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.net.exception.UnsupportedBodyTypeException;
import org.hashdb.ms.net.nio.SessionMeta;
import org.hashdb.ms.net.nio.msg.v1.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Date: 2024/1/18 18:35
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
public class HashV1ProtocolCodec implements ProtocolCodec {

    public enum Codec {
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

        private final MessageDecoder<?> factory;

        private final BodyParser bodyParser;

        private final boolean act;

        private static final Codec[] ENUM_MAP = values();

        Codec(Class<? extends Message<?>> messageClass) {
            this(messageClass, BodyParser.JSON);
        }

        public static Codec match(MessageMeta meta) {
            return ENUM_MAP[meta.key()];
        }

        @SuppressWarnings("unchecked")
        <M extends Message<?>> Codec(Class<M> messageClass, BodyParser bodyParser) {
            this(bodyParser, new MessageConstructorDecoder<>(messageClass, messageConstructor -> messageConstructor.getParameterTypes()[messageConstructor.getParameterCount() - 1], bodyParser));
        }

        Codec(BodyParser bodyParser, MessageConstructorDecoder<?> factory) {
            this.bodyParser = bodyParser;
            act = factory.isAct();
            this.factory = factory;
        }

        <M extends Message<?>> Codec(Class<M> messageClass, BodyParser bodyParser, MessageDecoder<? extends M> factory) {
            this.bodyParser = bodyParser;
            act = ActMessage.class.isAssignableFrom(messageClass);
            this.factory = factory;
        }

        public BodyParser bodyParser() {
            return bodyParser;
        }

        public boolean isAct() {
            return act;
        }

        public interface MessageDecoder<M extends Message<?>> {
            M decode(long id, ByteBuf buf);
        }

        public static class MessageConstructorDecoder<M extends Message<?>> implements MessageDecoder<M> {
            private final Constructor<M> messageConstructor;

            private final Class<?> messageBodyClass;

            private final BodyParser bodyParser;

            private final boolean isAct;

            public MessageConstructorDecoder(Class<M> messageClass, Function<Constructor<M>, Class<?>> messageBodyFinder, BodyParser bodyParser) {
                this.messageConstructor = selectConstructor(messageClass);
                this.messageBodyClass = messageBodyFinder.apply(this.messageConstructor);
                this.isAct = messageClass.isAssignableFrom(ActMessage.class);
                this.bodyParser = bodyParser;
            }


            private static <M extends Message<?>> Constructor<M> selectConstructor(Class<M> messageClass) {
                boolean isAct = messageClass.isAssignableFrom(ActMessage.class);
                @SuppressWarnings("unchecked")
                var constructors = (((Constructor<M>[]) messageClass.getConstructors()));
                Predicate<Constructor<M>> filter;
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
                    Constructor<M> messageConstructor = Arrays.stream(constructors).filter(filter).findFirst().orElseThrow(() -> {
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
                    log.error(e.getMessage(), e);
                    System.exit(1);
                    throw new DBSystemException(e);
                }
            }


            public boolean isAct() {
                return isAct;
            }

            @Override
            public M decode(long id, ByteBuf buf) {
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

        public Message<?> decode(long id, ByteBuf buf) {
            return factory.decode(id, buf);
        }
    }

    @Override
    public @NotNull ByteBuf encode(ChannelHandlerContext ctx, Message<?> msg) {
        Codec codec = Codec.match(msg.getMeta());
        var body = codec.bodyParser().encode(msg.body());
        var out = ctx.alloc().buffer(body.readableBytes() + 1 + 30);
        // 4 message meta(message type info)
        out.writeInt(msg.getMeta().key());
        // 8 message id
        out.writeLong(msg.id());
        // 4 body length
        // 如果是应答类消息, 那么body前8个字节就为actId
        if (codec.isAct()) {
            // expand body length for actId
            out.writeInt(body.readableBytes() + 1 + 8);
            // 4 act message id
            out.writeLong(((ActMessage<?>) msg).actId());
        } else {
            out.writeInt(body.readableBytes() + 1);
        }
        // [body length] body
        out.writeBytes(body);
        return out;
    }

    @Override
    public @Nullable Message<?> decode(ChannelHandlerContext ctx, ByteBuf in) {
        // check message
        if (in.readableBytes() < 16) {
            var bytes = new byte[Math.min(in.readableBytes(), 90)];
            in.readBytes(bytes);
            log.warn("illegal message. buf: {} content: '{}'", in, new String(bytes));
            return null;
        }
        var messageMeta = ProtocolCodec.resolveMessageMeta(ctx, in);
        if (messageMeta == null) {
            return null;
        }
        var id = in.readLong();
        // 略过body长度, 这个body长度是在LengthFieldBasedFrameDecoder被用到的字段
        in.readerIndex(in.readerIndex() + 4);
        // parse body
        try {
            return Codec.match(messageMeta).decode(id, in);
        } catch (Exception e) {
            log.warn("MESSAGE PARSE ERROR", e);
            ctx.write(new UnsupportedBodyTypeException("body pase error").msg(0));
            return null;
        }
    }
}
