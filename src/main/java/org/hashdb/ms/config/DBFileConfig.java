package org.hashdb.ms.config;

import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.aspect.methodAccess.DisposableUse;
import org.hashdb.ms.persistent.FileUtils;
import org.hashdb.ms.util.Lazy;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

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
public class DBFileConfig  implements InitializingBean {
    private String filepath;
    private long chunkSize;
    private long saveInterval;
    private final Lazy<File> dbFileRootDir = Lazy.of(()-> Path.of(filepath).normalize().toFile());
    /**
     * 属性注入完成后, 检查 dbFileDir 是否存在
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        File rootDir = dbFileRootDir.get();
        FileUtils.prepareDir(rootDir,
                ()-> new RuntimeException("Create data file directory failed! may be it is existed but it isn`t a directory. root path: '"+ rootDir
                +"' .config path:"+filepath)
        );
        log.info("config: {}", Map.of(
                "filepath", dbFileRootDir.get().getAbsolutePath(),
                "chunkSize", chunkSize + " byte",
                "saveInterval", chunkSize + " ms"
        ));
    }

    public File getDbFileRootDir() {
        return dbFileRootDir.get();
    }

    @DisposableUse
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    @DisposableUse
    public void setChunkSize(String chunkSize) {
        this.chunkSize = parseToLong(chunkSize);
    }
    @DisposableUse
    public void setSaveInterval(String saveInterval) {
        this.saveInterval = parseToLong(saveInterval);
    }
    public static long parseToLong(String exp) {
        Expression expression = new SpelExpressionParser().parseExpression(exp);
        Object value = expression.getValue();
        if(value == null) {
            throw new RuntimeException("can`t parse exp: '"+exp+"'. the expression return null value");
        }
        return Long.parseLong(value.toString());
    }
}
