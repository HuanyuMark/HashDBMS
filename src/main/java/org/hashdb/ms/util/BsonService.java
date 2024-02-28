package org.hashdb.ms.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.undercouch.bson4jackson.BsonFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * Date: 2024/2/27 23:30
 *
 * @author Huanyu Mark
 */
public class BsonService {
    public static final ObjectMapper COMMON;

    static {
        var factory = new BsonFactory();
        COMMON = new ObjectMapper(factory);
    }

    public static void transfer(Object content, File file) throws IOException {
        var out = Files.newOutputStream(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE);
        COMMON.writeValue(out, content);
    }
}
