package org.hashdb.ms.persistent.aof;

import org.hashdb.ms.data.Database;
import org.hashdb.ms.support.MultiArgsObjectProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;

/**
 * Date: 2024/3/6 17:09
 *
 * @author Huanyu Mark
 */
@Component
public class AofFlusherProvider implements MultiArgsObjectProvider<AofFlusher> {

    private final AofManager aofManager;

    public AofFlusherProvider(@Autowired(required = false) AofManager aofManager) {
        this.aofManager = aofManager;
    }

    @Override
    public @NotNull AofFlusher getObject(Object @NotNull ... args) throws BeansException {
        if (aofManager == null) {
            return NopAofFlusher.get();
        }
        if (args.length < 1) {
            throw new IllegalArgumentException("args length must be > 1");
        }
        if (args[0] instanceof Database database) {
            return aofManager.get(database);
        }
        if (args[0] instanceof Path path) {
            return aofManager.get(path);
        }
        if (args[0] instanceof File file) {
            return aofManager.get(file.toPath());
        }
        throw new IllegalArgumentException("args[0] must be Database or Path or File");
    }
}
