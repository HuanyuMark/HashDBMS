package org.hashdb.ms.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.exception.RequiredConfigException;
import org.hashdb.ms.persistent.FileUtils;
import org.hashdb.ms.support.Exit;

import java.io.File;
import java.nio.file.Path;

/**
 * Date: 2023/12/5 17:09
 *
 * @author Huanyu Mark
 */
@Data
@Slf4j
public abstract class PersistentConfig {
    protected String path;
    protected final long chunkSize;
    protected final long saveInterval;

    private final File rootDir;

    public PersistentConfig(String path, Long chunkSize, Long saveInterval) {
        if (path == null) {
            log.error(RequiredConfigException.of("db.file.path").getMessage());
            throw Exit.exception();
        }
        this.path = path;
        this.rootDir = FileUtils.prepareDir(Path.of(path).normalize().toFile(),
                () -> new DBSystemException("Create persistent file directory failed! may be it is existed but it isn`t a directory. root path: '" + path + "'"));
        this.chunkSize = chunkSize == null ? 1024 * 1024 : chunkSize;
        this.saveInterval = saveInterval == null ? 1000 * 60 * 60 * 24 : saveInterval;
    }

    public File getRootDir() {
        return rootDir;
    }
}
