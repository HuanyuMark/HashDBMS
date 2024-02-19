package org.hashdb.ms.net.nio.protocol;

import org.hashdb.ms.net.exception.UnsupportedProtocolException;
import org.hashdb.ms.net.nio.MetaEnum;

/**
 * Date: 2024/1/17 12:10
 *
 * @author huanyuMake-pecdle
 */
public enum Protocol implements MetaEnum {
    HASH_V1(new HashV1ProtocolCodec()),
    ;

    private static final Protocol[] ENUM_MAP = values();
    private final ProtocolCodec codec;

    Protocol(ProtocolCodec codec) {
        this.codec = codec;
    }

    public static Protocol resolve(int b) throws UnsupportedProtocolException {
        try {
            return ENUM_MAP[b];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw UnsupportedProtocolException.unsupported(b);
        }
    }

    public ProtocolCodec codec() {
        return codec;
    }

    @Override
    public int key() {
        return ordinal();
    }
}
