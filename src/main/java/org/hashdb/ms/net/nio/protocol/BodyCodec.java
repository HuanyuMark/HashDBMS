package org.hashdb.ms.net.nio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.EmptyByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.net.exception.UnsupportedBodyTypeException;
import org.hashdb.ms.net.nio.MetaEnum;
import org.hashdb.ms.net.nio.SessionMeta;
import org.hashdb.ms.util.JsonService;

import java.io.IOException;

/**
 * Date: 2024/1/17 12:09
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
public enum BodyCodec implements MetaEnum {
    JSON(JsonService::toByteBuf, (bodyClass, buf) -> {
        try {
            return JsonService.parse(new ByteBufInputStream(buf), bodyClass);
        } catch (IOException e) {
            throw new DBSystemException(e);
        }
    }),
    NULL(BodyCodec::emptyBody, (bodyClass, buf) -> null),
    SESSION_META(msg -> {
        if (!(msg instanceof SessionMeta meta)) {
            throw new IllegalArgumentException("BodyParser 'SESSION_META' can not serialize '" + msg + "', expect type: 'SESSION_META'");
        }
        var buf = ByteBufAllocator.DEFAULT.buffer(4);
        buf.writeInt(meta.key());
        return buf;
    }, (bodyClass, buf) -> SessionMeta.resolve(buf.readInt()));

    private static ByteBuf EMPTY_BODY = emptyBody();

    private static ByteBuf emptyBody(Object any) {
        if (EMPTY_BODY == null) {
            EMPTY_BODY = new EmptyByteBuf(ByteBufAllocator.DEFAULT);
        }
        return EMPTY_BODY;
    }

    public static ByteBuf emptyBody() {
        return emptyBody(null);
    }

    private final Encoder encoder;

    private final Decoder decoder;

    static final BodyCodec[] ENUM_MAP = BodyCodec.values();

    @Override
    public int key() {
        return ordinal();
    }

    interface Encoder {
        ByteBuf encode(Object body);
    }

    interface Decoder {
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

    public ByteBuf encode(Object body) {
        if (body == null) {
            return EMPTY_BODY;
        }
        return encoder.encode(body);
    }

    public Object decode(Class<?> bodyClass, ByteBuf buf) {
        return decoder.decode(bodyClass, buf);
    }
}
