package org.hashdb.ms.net.nio.protocol;

import io.netty.buffer.ByteBuf;
import org.hashdb.ms.net.exception.UnsupportedProtocolException;
import org.hashdb.ms.net.nio.MetaEnum;
import org.hashdb.ms.net.nio.msg.v1.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date: 2024/1/17 12:10
 *
 * @author Huanyu Mark
 */
public enum Protocol implements MetaEnum {
    HASH_V1(new HashV1MessageCodec()),
    ;

    /**
     * 享元模式, 用于缓存实例不多的Message的ByteBuf.
     * 如果消息的实例过多可能会发生内存泄漏
     */
    private final Map<Message<?>, ByteBuf> sharedMessageByteBufMap = new ConcurrentHashMap<>();

    private static final Protocol[] ENUM_MAP = values();
    private final MessageCodec codec;

    Protocol(MessageCodec codec) {
        this.codec = codec;
    }

    public static Protocol resolve(int b) throws UnsupportedProtocolException {
        try {
            return ENUM_MAP[b];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw UnsupportedProtocolException.unsupported(b);
        }
    }

    public MessageCodec codec() {
        return codec;
    }

    public Map<Message<?>, ByteBuf> sharedMessageByteBufMap() {
        return sharedMessageByteBufMap;
    }

    @Override
    public int key() {
        return ordinal();
    }
}
