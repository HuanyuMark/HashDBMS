package org.hashdb.ms.persistent.sys;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.HdbConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.DatabaseInfos;
import org.hashdb.ms.persistent.hdb.AbstractHdb;
import org.hashdb.ms.persistent.hdb.Hdb;
import org.hashdb.ms.support.Exit;
import org.hashdb.ms.util.AtomLazy;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.ObjectProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Date: 2024/3/5 22:53
 *
 * @author Huanyu Mark
 */
@Slf4j
@RequiredArgsConstructor
public class HSystemProtocolV1HdbSystemPersistService extends HSystemProtocolV1SystemPersistService implements SystemPersistService {
    public static final int version = 1;

    private final HdbConfig hdbConfig;

    private final ObjectProvider<AbstractHdb> hdbProvider;

    @Override
    protected Path findSystemInfoFile() throws IOException {
        assert hdbConfig.getRootPath() != null;
        return Path.of(hdbConfig.getRootPath().toString(), hdbConfig.getSystemInfoFileName());
    }

    @Override
    protected DatabaseInfos scanDatabaseInfo(File dbDir) {
        var abstractHdb = hdbProvider.getObject(dbDir);
        if (!(abstractHdb instanceof Hdb hdb)) {
            throw new UnsupportedOperationException("can not load db without set 'db.file.path'");
        }
        return hdb.getDatabase().getInfos();
    }

    @Override
    @NotNull
    protected Lazy<Database> createDatabaseLoader(File dbDir) {
        var abstractHdb = hdbProvider.getObject(dbDir);
        if (!(abstractHdb instanceof Hdb hdb)) {
            throw new UnsupportedOperationException("can not load db without set 'db.file.path'");
        }
        Lazy<Database> dbLoader;
        if (Hdb.getHdbConfig().isLazyLoad()) {
            dbLoader = AtomLazy.of(() -> {
                try {
                    hdb.loadData();
                } catch (IOException e) {
                    throw Exit.error(log, "load database failed", e);
                }
                return hdb.getDatabase();
            });
        } else {
            try {
                hdb.loadData();
            } catch (IOException e) {
                throw Exit.error(log, "load database failed", e);
            }
            dbLoader = Lazy.of(hdb.getDatabase());
        }
        return dbLoader;
    }
}
