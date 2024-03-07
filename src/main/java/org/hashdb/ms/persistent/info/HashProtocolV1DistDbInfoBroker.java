package org.hashdb.ms.persistent.info;

import io.netty.buffer.ByteBufAllocator;
import org.hashdb.ms.data.DatabaseInfos;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Date: 2024/3/6 0:32
 *
 * @author Huanyu Mark
 */
public class HashProtocolV1DistDbInfoBroker implements DbInfoBroker {

    private final Path infosPath;

    private final Charset charset;

    public HashProtocolV1DistDbInfoBroker(Path infosPath, Charset charset) {
        this.infosPath = infosPath;
        this.charset = charset;
    }

    public DatabaseInfos readInfos() throws IOException {
        var buffer = ByteBufAllocator.DEFAULT.buffer();
        try (var in = Files.newInputStream(infosPath, StandardOpenOption.WRITE)) {
            buffer.writeBytes(in, in.available());
        }
        try (var base = new HashProtocolV1ByteBufDbInfoBroker(buffer, charset)) {
            return base.readInfos();
        }
    }

    @Override
    public void writeInfos(DatabaseInfos infos) throws IOException {
        int id = infos.getId();
        var dbName = infos.getName();
        var createTime = infos.getCreateTime();
        var buffer = ByteBufAllocator.DEFAULT.buffer();
        try (var base = new HashProtocolV1ByteBufDbInfoBroker(buffer, charset)) {
            base.writeInfos(infos);
            try (var out = Files.newOutputStream(infosPath, StandardOpenOption.WRITE)) {
                buffer.readBytes(out, buffer.readableBytes());
            }
        }
    }
}
