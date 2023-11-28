package org.hashdb.ms.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.aspect.methodAccess.ConfigLoadOnly;
import org.hashdb.ms.exception.RequiredConfigException;
import org.hashdb.ms.persistent.FileUtils;
import org.hashdb.ms.util.JacksonSerializer;
import org.hashdb.ms.util.Lazy;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

/**
 * Date: 2023/11/21 12:26
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
@Getter
@Configuration
@ConfigurationProperties("db.file")
@EnableConfigurationProperties
public class DBFileConfig implements InitializingBean {
    private String filepath;
    private long chunkSize = 1 * 1024 * 1024;
    private long saveInterval = 1000 * 60 * 60 * 24;
    private final Lazy<File> dbFileRootDir = Lazy.of(() -> FileUtils.prepareDir(Path.of(filepath).normalize().toFile(),
            ()-> new RuntimeException("Create data file directory failed! may be it is existed but it isn`t a directory. root path: '" + filepath + "'"))
    );

    private final String systemInfoFileName = "sys.info";

    /**
     * true:
     * 在第一次访问数据库时, 才从磁盘中加载数据入内存
     * false:
     * 在启动数据库服务器时, 就将说有的数据从磁盘读入内存
     */
    private boolean lazyLoad = true;
    /**
     * 属性注入完成后, 检查 dbFileDir 是否存在
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if(filepath == null) {
            throw RequiredConfigException.of("db.file.filepath");
        }
        File rootDir = dbFileRootDir.get();
        FileUtils.prepareDir(rootDir,
                () -> new RuntimeException("Create data file directory failed! may be it is existed but it isn`t a directory. root path: '" + rootDir
                        + "' .config path:" + filepath)
        );
        log.info("db file config: {}", JacksonSerializer.stringfy(Map.of(
                "filepath", dbFileRootDir.get().getAbsolutePath(),
                "chunkSize", chunkSize + " byte",
                "saveInterval", chunkSize + " ms"
        )));
    }

    public File getDbFileRootDir() {
        return dbFileRootDir.get();
    }

    @ConfigLoadOnly
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    @ConfigLoadOnly
    public void setChunkSize(String chunkSize) {
        this.chunkSize = parseToLong(chunkSize);
    }

    @ConfigLoadOnly
    public void setLazyLoad(boolean lazyLoad) {
        this.lazyLoad = lazyLoad;
    }

    @ConfigLoadOnly
    public void setSaveInterval(String saveInterval) {
        this.saveInterval = parseToLong(saveInterval);
    }

    public static long parseToLong(String exp) {
        Expression expression = new SpelExpressionParser().parseExpression(exp);
        Object value = expression.getValue();
        if (value == null) {
            throw new RuntimeException("can`t parse exp: '" + exp + "'. the expression return null value");
        }
        return Long.parseLong(value.toString());
    }

    public String getSystemInfoFileName() {
        return systemInfoFileName;
    }
}
