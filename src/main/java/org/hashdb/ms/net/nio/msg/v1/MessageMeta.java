package org.hashdb.ms.net.nio.msg.v1;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.net.exception.IllegalMessageException;
import org.hashdb.ms.net.nio.MetaEnum;

/**
 * Date: 2024/1/16 21:35
 *
 * @author huanyuMake-pecdle
 */
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
    PING(ServerPingMessage.class),
    /**
     * 心跳，收
     */
    PONG(ServerPongMessage.class),
    PROTOCOL_SWITCHING(ProtocolSwitchingMessage.class),
    RECONNECT(ReconnectMessage.class),

    SESSION_STATE(SessionStateMessage.class),
    /**
     * 切换会话的类型
     */
    SESSION_UPGRADE(SessionUpgradeMessage.class);
    private static final MessageMeta[] ENUM_MAP = values();

    private final Class<? extends Message<?>> messageClass;

    MessageMeta(Class<? extends Message<?>> messageClass) {
        this.messageClass = messageClass;
    }

    public static MessageMeta resolve(int messageMetaKey) throws IllegalMessageException {
        try {
            return ENUM_MAP[messageMetaKey];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalMessageException("illegal message type '" + messageMetaKey + "'");
        }
    }

    public Class<? extends Message<?>> messageClass() {
        return messageClass;
    }

    @Override
    public int key() {
        return ordinal();
    }
}
