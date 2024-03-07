package org.hashdb.ms.persistent.hdb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;

/**
 * Date: 2024/3/5 14:39
 * 读写HDB文件(线程不安全)
 * 使用自定义的 HDB 协议进行序列化和反序列化
 *
 * @author Huanyu Mark
 */
@Slf4j
public class HdbPersistServiceFactory {
    @Getter
    private static final Charset charset = StandardCharsets.UTF_8;

    public static DbPersistService create(Hdb hdb) throws IOException {
        try (var file = new RandomAccessFile(hdb.getDataPath().toFile(), "r")) {
            var version = file.readByte();
            var service = create(hdb, version);
            if (service != null) return service;
        } catch (FileNotFoundException not1) {
            try (var channel = FileChannel.open(hdb.getInfosPath(), StandardOpenOption.READ)) {
                var buffer = ByteBuffer.allocate(1);
                channel.read(buffer);
                byte version = buffer.get(0);
                var service = create(hdb, version);
                if (service != null) return service;
            } catch (FileNotFoundException not2) {
                throw new FileNotFoundException(STR."can not found file '\{hdb.getInfosPath()}' and '\{hdb.getDataPath()}'");
            }
        }
        throw new IOException("Unsupported HDB version");
    }

    @Nullable
    private static DbPersistService create(Hdb hdb, byte version) {
        if (version == HdbProtocolV1DbPersistService.version) {
            return new HdbProtocolV1DbPersistService(hdb);
        }
        if (version == JavaProtocolV1DbPersistService.version) {
            return new JavaProtocolV1DbPersistService(hdb);
        }
        return null;
    }
}
