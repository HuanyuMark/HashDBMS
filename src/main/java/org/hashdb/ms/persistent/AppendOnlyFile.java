package org.hashdb.ms.persistent;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.exception.DBSystemException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Date: 2024/1/7 0:34
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
class AppendOnlyFile {
    private final File content;
    private final List<String> cache = new LinkedList<>();

    private final int order;

    AppendOnlyFile(File content, int order) {
        this.content = content;
        this.order = order;
    }

    public boolean hasCache() {
        return !cache.isEmpty();
    }

    public File file() {
        return content;
    }

    public int order() {
        return order;
    }

    public void append(String command) {
        cache.add(command);
    }

    public synchronized void store() {
        if (cache.isEmpty()) {
            return;
        }
        try (
                FileWriter writer = new FileWriter(content, true);
        ) {
            for (String command : cache) {
                writer.append(command).append('\n');
            }
            cache.clear();
        } catch (IOException e) {
            log.error("can not store aof file '{}'", content, e);
            throw new DBSystemException(e);
        }
    }
}
