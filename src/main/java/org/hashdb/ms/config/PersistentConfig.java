package org.hashdb.ms.config;

import jakarta.annotation.Resource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.exception.RequiredConfigException;
import org.hashdb.ms.persistent.FileUtils;
import org.hashdb.ms.support.Exit;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.AnnotationUtils;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Date: 2023/12/5 17:09
 *
 * @author Huanyu Mark
 */
@Slf4j
@Getter
public class PersistentConfig {

    @Resource
    @Getter(AccessLevel.PROTECTED)
    private DefaultConfig defaultConfig;
    /**
     * 如果这两个其中之一为null, 则说明不使用这种持久化方式
     */
    protected final @Nullable Path rootPath;

    private final @Nullable File rootDir;

    public PersistentConfig(String rootPath, boolean required) {
        if (rootPath == null || rootPath.trim().isEmpty()) {
            if (required) {
                throw Exit.error(log, "config loading failed!", RequiredConfigException.of("db.file.path"));
            }
            rootDir = null;
            this.rootPath = null;
            return;
        }
        try {
            this.rootPath = Path.of(rootPath).normalize();
        } catch (InvalidPathException e) {
            throw Exit.error(log, "config loading failed!", STR."invalid value '\{rootPath}' of option '\{configPrefix()}.path'");
        }
        this.rootDir = FileUtils.prepareDir(this.rootPath.toFile(),
                () -> new DBSystemException(STR."Create persistent file directory failed! may be it is existed but it isn`t a directory. root path: '\{this.rootPath}'"));
    }

    private String configPrefix() {
        return Objects.requireNonNull(AnnotationUtils.findAnnotation(getClass(), ConfigurationProperties.class), STR."'\{getClass()}' should be annotate with '\{PersistentConfig.class}'").value();
    }
}
