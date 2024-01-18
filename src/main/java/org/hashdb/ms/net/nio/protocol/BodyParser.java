package org.hashdb.ms.net.nio.protocol;

import org.hashdb.ms.net.exception.UnsupportedBodyTypeException;
import org.hashdb.ms.util.JsonService;

import java.io.IOException;

/**
 * Date: 2024/1/17 12:09
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public enum BodyParser {
    JSON(JsonService::toBytes, (source, offset, length) -> {
        try {
            return JsonService.parse(source, offset, length, Object.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    });

    private static final byte[] EMPTY_BODY = new byte[0];

    private final Encoder encoder;

    private final Decoder decoder;

    static final BodyParser[] constant = BodyParser.values();

    interface Encoder {
        byte[] encode(Object body);
    }

    interface Decoder {
        Object decode(byte[] source, int offset, int length);
    }

    BodyParser(Encoder encoder, Decoder decoder) {
        this.encoder = encoder;
        this.decoder = decoder;
    }

    public static BodyParser ofCode(byte b) throws UnsupportedBodyTypeException {
        try {
            return constant[b];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw UnsupportedBodyTypeException.unsupported(b);
        }
    }

    public byte[] encode(Object body) {
        if (body == null) {
            return EMPTY_BODY;
        }
        return encoder.encode(body);
    }

    public Object decode(byte[] source, int offset, int length) {
        return decoder.decode(source, offset, length);
    }
}
