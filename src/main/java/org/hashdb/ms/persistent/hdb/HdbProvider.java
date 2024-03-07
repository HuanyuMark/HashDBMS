package org.hashdb.ms.persistent.hdb;

import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.support.MultiArgsObjectProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Date: 2024/3/6 17:04
 *
 * @author Huanyu Mark
 */
@Component
public class HdbProvider implements MultiArgsObjectProvider<AbstractHdb> {

    private final HdbManager hdbManager;

    public HdbProvider(@Autowired(required = false) HdbManager hdbManager) {
        this.hdbManager = hdbManager;
    }

    @Override
    public @NotNull AbstractHdb getObject(Object @NotNull ... args) throws BeansException {
        if (hdbManager == null) {
            return NopHdb.get();
        }
        if (args.length < 1) {
            throw new DBSystemException("args length must be 1");
        }
        if (args[0] instanceof Database database) {
            return hdbManager.get(database);
        }
        try {
            if (args[0] instanceof File dbDir) {
                return hdbManager.preload(dbDir);
            }
            if (args[0] instanceof Path path) {
                return hdbManager.preload(path.toFile());
            }
        } catch (IOException e) {
            throw new BeanCreationException("preload IO error", e);
        }
        throw new DBSystemException("args type must be File or Database");
    }
}
