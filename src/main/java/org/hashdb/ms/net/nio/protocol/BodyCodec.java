package org.hashdb.ms.net.nio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.net.exception.UnsupportedBodyTypeException;
import org.hashdb.ms.net.nio.MetaEnum;
import org.hashdb.ms.net.nio.SessionMeta;
import org.hashdb.ms.util.JsonService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Date: 2024/1/17 12:09
 *
 * @author Huanyu Mark
 */
@Slf4j
public enum BodyCodec implements MetaEnum {
    JSON(JsonService::transferTo, (bodyClass, buf) -> {
        try {
            return JsonService.parse(new ByteBufInputStream(buf), bodyClass);
        } catch (IOException e) {
            throw new DBSystemException(e);
        }
    }),
    NULL((any, in) -> {
    }, (bodyClass, buf) -> null),
    SESSION_META((msg, buf) -> {
        if (!(msg instanceof SessionMeta meta)) {
            throw new IllegalArgumentException(STR."BodyParser 'SESSION_META' can not serialize '\{msg}', expect type: 'SESSION_META'");
        }
        buf.writeInt(meta.key());
    }, (bodyClass, buf) -> SessionMeta.resolve(buf.readInt())),
    STRING((msg, buf) -> {
        if (!(msg instanceof CharSequence sequence)) {
            throw new IllegalArgumentException(STR."BodyParser 'STRING' can not serialize '\{msg}', expect type: 'String'");
        }
        buf.writeCharSequence(sequence, StandardCharsets.UTF_8);
    }, (bodyClass, buf) -> buf.readCharSequence(buf.readableBytes(), StandardCharsets.UTF_8));

    private final Encoder encoder;

    private final Decoder decoder;

    static final BodyCodec[] ENUM_MAP = BodyCodec.values();

    @Override
    public int key() {
        return ordinal();
    }

    private interface Encoder {
        void encode(Object body, ByteBuf in);
    }

    private interface Decoder {
        Object decode(Class<?> bodyClass, ByteBuf buf);
    }

    BodyCodec(Encoder encoder, Decoder decoder) {
        this.encoder = encoder;
        this.decoder = decoder;
    }

    public static BodyCodec resolve(byte b) throws UnsupportedBodyTypeException {
        try {
            return ENUM_MAP[b];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw UnsupportedBodyTypeException.unsupported(b);
        }
    }

    public void encode(Object body, ByteBuf buf) {
        encoder.encode(body, buf);
    }

    public Object decode(Class<?> bodyClass, ByteBuf buf) {
        return decoder.decode(bodyClass, buf);
    }
}
