package org.hashdb.ms.persistent.aof;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.hashdb.ms.persistent.info.HashProtocolV1ByteBufDbInfoBroker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Date: 2024/2/29 13:50
 * <p>
 *
 * @author Huanyu Mark
 */
public class ReadonlyAof extends ReadableAof {

    private final ByteBuf buffer;
    private final int readerIndex;

    public ReadonlyAof(ByteBuf buffer) {
        super(new HashProtocolV1ByteBufDbInfoBroker(buffer, StandardCharsets.UTF_8));
        this.buffer = buffer;
        readerIndex = buffer.readerIndex();
    }

    @Override
    public LineReader newReader() {
        buffer.readerIndex(readerIndex);
        return new BufferedReaderAdaptor(new BufferedReader(new InputStreamReader(new ByteBufInputStream(buffer), StandardCharsets.UTF_8)));
    }
}
