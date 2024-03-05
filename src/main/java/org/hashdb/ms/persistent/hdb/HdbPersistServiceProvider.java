package org.hashdb.ms.persistent.hdb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Date: 2024/3/5 14:39
 * 读写HDB文件(线程不安全)
 * 使用自定义的 HDB 协议进行序列化和反序列化
 *
 * @author Huanyu Mark
 */
@Slf4j
public class HdbPersistServiceProvider {
    @Getter
    private static final Charset charset = StandardCharsets.UTF_8;

    public static HdbPersistService provide(Path filePath) throws IOException {
        try (var file = new RandomAccessFile(filePath.toFile(), "r")) {
            if (file.readByte() == 1) {
                return new HdbProtocolV1HdbPersistService(filePath);
            }
        }
        throw new IOException("Unsupported HDB version");
    }
}
