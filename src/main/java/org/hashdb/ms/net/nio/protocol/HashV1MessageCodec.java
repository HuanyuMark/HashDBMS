package org.hashdb.ms.net.nio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.net.exception.UnsupportedBodyTypeException;
import org.hashdb.ms.net.nio.SessionMeta;
import org.hashdb.ms.net.nio.msg.v1.*;
import org.hashdb.ms.support.Exit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Date: 2024/1/18 18:35
 *
 * @author Huanyu Mark
 */
@Slf4j
public class HashV1MessageCodec implements MessageCodec {

    private enum CodecContext {
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
        PING(ServerPingMessage.class, BodyCodec.NULL, (id, bodyCodec, buf) -> ServerPingMessage.DEFAULT),
        /**
         * 心跳，收
         */
        PONG(ServerPongMessage.class, BodyCodec.NULL, (id, bodyCodec, buf) -> ServerPongMessage.DEFAULT),
        PROTOCOL_SWITCHING(ProtocolSwitchingMessage.class),
        RECONNECT(ReconnectMessage.class),

        SESSION_STATE(SessionStateMessage.class),
        /**
         * 切换会话的类型
         */
        SESSION_UPGRADE(SessionUpgradeMessage.class, BodyCodec.SESSION_META,
                (id, bodyCodec, buf) -> new SessionUpgradeMessage(((SessionMeta) bodyCodec.decode(SessionMeta.class, buf)))),

        SYNC(SyncMessage.class);

        private final MessageDecoder<?> decoder;

        private final BodyCodec bodyCodec;

        @Getter
        private final boolean act;

        private static final CodecContext[] ENUM_MAP = values();

        CodecContext(Class<? extends Message<?>> messageClass) {
            this(messageClass, selectDefaultBodyCodec(messageClass));
        }

        private static BodyCodec selectDefaultBodyCodec(Class<? extends Message<?>> messageClass) {
            var type = ResolvableType.forClass(messageClass);
            var generic = type.getSuperType().getGeneric(0);
            var bodyClass = generic.resolve();
            if (bodyClass != null && CharSequence.class.isAssignableFrom(bodyClass)) {
                return BodyCodec.STRING;
            }
            return BodyCodec.JSON;
        }

        public static CodecContext match(MessageMeta meta) {
            return ENUM_MAP[meta.key()];
        }

        <M extends Message<?>> CodecContext(Class<M> messageClass, BodyCodec bodyCodec) {
            this(bodyCodec, new MessageConstructorDecoder<>(messageClass, messageConstructor -> messageConstructor.getParameterTypes()[messageConstructor.getParameterCount() - 1], bodyCodec));
        }

        CodecContext(BodyCodec bodyCodec, MessageConstructorDecoder<?> decoder) {
            this.bodyCodec = bodyCodec;
            act = decoder.isAct();
            this.decoder = decoder;
        }

        <M extends Message<?>> CodecContext(Class<M> messageClass, BodyCodec bodyCodec, MessageDecoder<? extends M> decoder) {
            this.bodyCodec = bodyCodec;
            act = ActMessage.class.isAssignableFrom(messageClass);
            this.decoder = decoder;
        }

        public void writeBody(Object body, ByteBuf buf) {
            bodyCodec.encode(body, buf);
        }

        public interface MessageDecoder<M extends Message<?>> {
            M decode(int id, BodyCodec bodyCodec, ByteBuf buf);
        }

        public static class MessageConstructorDecoder<M extends Message<?>> implements MessageDecoder<M> {
            private final Constructor<M> messageConstructor;

            private final Class<?> messageBodyClass;

            private final boolean isAct;

            public MessageConstructorDecoder(Class<M> messageClass, Function<Constructor<M>, Class<?>> messageBodyFinder, BodyCodec bodyCodec) {
                this.isAct = messageClass.isAssignableFrom(ActMessage.class);
                this.messageConstructor = selectConstructor(messageClass);
                this.messageBodyClass = messageBodyFinder.apply(this.messageConstructor);
            }

            private Constructor<M> selectConstructor(Class<M> messageClass) {
                @SuppressWarnings("unchecked")
                var constructors = (((Constructor<M>[]) messageClass.getConstructors()));
                try {
                    Constructor<M> messageConstructor = Arrays.stream(constructors).filter(constructorFilter(isAct)).findFirst().orElseThrow(() -> {
                        if (isAct) {
                            return new NoSuchMethodException(
                                    STR."class '\{messageClass}' definition is illegal. it should contain a constructor with parameters '[\{long.class},\{long.class},\{Object.class}]'"
                            );
                        }
                        return new NoSuchMethodException(
                                STR."class '\{messageClass}' definition is illegal. it should contain a constructor with parameters '[\{long.class},\{Object.class}]'"
                        );
                    });
                    messageConstructor.setAccessible(true);
                    return messageConstructor;
                } catch (NoSuchMethodException e) {
                    log.error(e.getMessage(), e);
                    throw Exit.exception();
                }
            }

            @NotNull
            private static <M extends Message<?>> Predicate<Constructor<M>> constructorFilter(boolean isAct) {
                if (isAct) {
                    return c -> {
                        if (c.getParameterCount() != 3) {
                            return false;
                        }
                        Class<?>[] parameterTypes = c.getParameterTypes();
                        return (parameterTypes[0] == int.class || parameterTypes[0] == Integer.class) &&
                                (parameterTypes[1] == int.class || parameterTypes[1] == Integer.class);
                    };
                }
                return c -> {
                    if (c.getParameterCount() != 2) {
                        return false;
                    }
                    Class<?>[] parameterTypes = c.getParameterTypes();
                    return parameterTypes[0] == int.class || parameterTypes[0] == Integer.class;
                };
            }

            public boolean isAct() {
                return isAct;
            }

            @Override
            public M decode(int id, BodyCodec bodyCodec, ByteBuf buf) {
                try {
                    if (isAct) {
                        var actId = buf.readInt();
                        var body = bodyCodec.decode(messageBodyClass, buf);
                        return messageConstructor.newInstance(id, actId, body);
                    }
                    var body = bodyCodec.decode(messageBodyClass, buf);
                    return messageConstructor.newInstance(id, body);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    log.error("create message error", e);
                    throw Exit.exception();
                }
            }
        }

        public Message<?> decode(int id, ByteBuf buf) {
            return decoder.decode(id, bodyCodec, buf);
        }
    }

    public HashV1MessageCodec() {
        checkCodecImpls(CodecContext.ENUM_MAP);
    }

    @Override
    public LengthFieldBasedFrameDecoder frameDecoder() {
        return new LengthFieldBasedFrameDecoder(20 * 1024, 9, 4, 0, 0, true);
    }

    @Override
    public void encode(ByteBuf out, Message<?> msg) {
        var codec = CodecContext.match(msg.getMeta());
        // [1] message meta(message type info)
        out.writeByte(msg.getMeta().key());
        // [4] message id
        out.writeInt(msg.id());
        int bodyLengthIndex = out.writerIndex();
        // [4] body length.
        // 先略过一个int
        out.writerIndex(out.writerIndex() + 4);
        if (codec.isAct()) {
            // [4] act message id
            out.writeInt(((ActMessage<?>) msg).actId());
        }
        // [body length] body
        codec.writeBody(msg.body(), out);
        int bodyLength;
        if (codec.isAct()) {
            // expand body length for actId
            bodyLength = out.writerIndex() - bodyLengthIndex;
        } else {
            bodyLength = out.writerIndex() - bodyLengthIndex - 4;
        }
        int newestWriteIndex = out.writerIndex();
        out.writerIndex(bodyLengthIndex);
        out.writeInt(bodyLength);
        out.writerIndex(newestWriteIndex);
    }

    @Override
    public @Nullable Message<?> decode(ChannelHandlerContext ctx, ByteBuf in) {
        // check message
        if (in.readableBytes() < 9) {
            var bytes = new byte[Math.min(in.readableBytes(), 90)];
            in.readBytes(bytes);
            log.warn("illegal message. buf: {} content: '{}'", in, new String(bytes));
            return null;
        }
        var messageMeta = MessageCodec.resolveMessageMeta(ctx, in.readByte());
        if (messageMeta == null) {
            return null;
        }
        var id = in.readInt();
        // 略过body长度字段, 这个body长度是在LengthFieldBasedFrameDecoder被用到的字段
        in.readerIndex(in.readerIndex() + 4);
        // parse body
        try {
            return CodecContext.match(messageMeta).decode(id, in);
        } catch (Exception e) {
            log.warn("MESSAGE PARSE ERROR", e);
            ctx.write(new UnsupportedBodyTypeException("body parse error").msg(0));
            return null;
        }
    }
}
