package org.hashdb.ms.net.nio.msg.v1;

import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.net.exception.IllegalMessageException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Date: 2024/1/16 21:35
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public enum MessageType {
    RECONNECT(ReconnectMessage.class),
    ERROR(ErrorMessage.class),
    ACT(DefaultActMessage.class),
    AUTHENTICATION(AuthenticationMessage.class),
    ACT_AUTHENTICATION(ActAuthenticationMessage.class),

    /**
     * 操作数据库,切换数据库,创建用户,登录时用的消息体
     */
    APP_COMMAND(AppCommandMessage.class),
    ACT_APP_COMMAND(ActAppCommandMessage.class);

    private final Class<? extends Message<?>> messageClass;

    private final Constructor<? extends Message<?>> messageConstructor;

    private final Message<?> singleton;

    private final boolean act;

    private static final MessageType[] messageTypeMap = values();

    MessageType(Class<? extends Message<?>> messageClass) {
        this(messageClass, null);
    }

    <M extends Message<?>> MessageType(Class<M> messageClass, M singleton) {
        this.messageClass = messageClass;
        this.singleton = singleton;
        act = messageClass.isAssignableFrom(ActMessage.class);
        messageConstructor = selectConstructor(messageClass);
    }

    private static Constructor<? extends Message<?>> selectConstructor(Class<? extends Message<?>> messageClass) {
        boolean isAct = messageClass.isAssignableFrom(ActMessage.class);
        @SuppressWarnings("unchecked")
        var constructors = (((Constructor<? extends Message<?>>[]) messageClass.getConstructors()));
        Predicate<Constructor<? extends Message<?>>> filter;
        if (isAct) {
            filter = c -> {
                Class<?>[] parameterTypes = c.getParameterTypes();
                if (parameterTypes.length != 3) {
                    return false;
                }
                if (parameterTypes[0] != long.class && parameterTypes[0] != Long.class ||
                        parameterTypes[1] != long.class && parameterTypes[1] != Long.class
                ) {
                    return false;
                }
                return Object.class.isAssignableFrom(parameterTypes[2]);
            };
        } else {
            filter = c -> {
                Class<?>[] parameterTypes = c.getParameterTypes();
                if (parameterTypes.length != 2) {
                    return false;
                }
                if (parameterTypes[0] != long.class && parameterTypes[0] != Long.class) {
                    return false;
                }
                return Object.class.isAssignableFrom(parameterTypes[1]);
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
        LoggerFactory.getLogger(MessageType.class).error(msg, e);
    }

    public static MessageType ofCode(int code) throws IllegalMessageException {
        try {
            return messageTypeMap[code];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalMessageException("illegal message type '" + code + "'");
        }
    }


    /**
     * 在调用前, 你必须要清楚, 能否实例化 act 类message, 预先使用 {@link #isActMessage()} 做检查
     * 因为实例化message所使用的构造器可能只适用于实例化 非 act 类message
     *
     * @param id    message id
     * @param actId act message id, 如果为null, 则表示以构造非act类message的方式实例化message
     * @param body  message body
     * @return message instance
     */
    public Message<?> create(long id, @Nullable Long actId, @Nullable Object body) {
        if (singleton != null) {
            return singleton;
        }
        try {
            if (actId != null) {
                return messageConstructor.newInstance(id, actId, body);
            }
            return messageConstructor.newInstance(id, body);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            error("instantiate message error", e);
            System.exit(1);
        }
        // unreachable
        return null;
    }


    public Class<? extends Message<?>> messaageClass() {
        return messageClass;
    }

    public boolean isActMessage() {
        return act;
    }
}
