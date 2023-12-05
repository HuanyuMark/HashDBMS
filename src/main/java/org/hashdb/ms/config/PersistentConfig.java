package org.hashdb.ms.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.aspect.methodAccess.ConfigLoadOnly;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.exception.RequiredConfigException;
import org.hashdb.ms.persistent.FileUtils;
import org.hashdb.ms.util.JsonService;
import org.hashdb.ms.util.Lazy;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

/**
 * Date: 2023/12/5 17:09
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Data
@Slf4j
public abstract class PersistentConfig implements InitializingBean {
    protected String path;
    protected long chunkSize = 1 * 1024 * 1024;
    protected long saveInterval = 1000 * 60 * 60 * 24;

    private final Lazy<File> rootDir = Lazy.of(() -> FileUtils.prepareDir(Path.of(path, rootDirName()).normalize().toFile(),
            () -> new DBSystemException("Create persistent file directory failed! may be it is existed but it isn`t a directory. root path: '" + path + "'"))
    );

    public static long parseToLong(String exp) {
        Expression expression = new SpelExpressionParser().parseExpression(exp);
        Object value = expression.getValue();
        if (value == null) {
            throw new RuntimeException("can`t parse exp: '" + exp + "'. the expression return null value");
        }
        return Long.parseLong(value.toString());
    }

    @ConfigLoadOnly
    public void setPath(String path) {
        this.path = path;
    }

    @ConfigLoadOnly
    public void setChunkSize(String chunkSize) {
        this.chunkSize = PersistentConfig.parseToLong(chunkSize);
    }

    @ConfigLoadOnly
    public void setSaveInterval(String saveInterval) {
        this.saveInterval = PersistentConfig.parseToLong(saveInterval);
    }

    abstract protected String rootDirName();

    /**
     * 属性注入完成后, 检查 dbFileDir 是否存在
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (path == null) {
            throw RequiredConfigException.of("db.file.path");
        }
        rootDir.get();
        log.info("db file config: {}", JsonService.stringfy(Map.of(
                "filepath", rootDir.get().getAbsolutePath(),
                "chunkSize", chunkSize + " byte",
                "saveInterval", chunkSize + " ms"
        )));
    }

    public File getRootDir() {
        return rootDir.get();
    }
}
