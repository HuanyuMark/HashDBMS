package org.hashdb.ms.persistent.sys;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.AofConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.DatabaseInfos;
import org.hashdb.ms.persistent.aof.Aof;
import org.hashdb.ms.persistent.aof.AofFlusher;
import org.hashdb.ms.util.AtomLazy;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.ObjectProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Date: 2024/3/6 1:12
 *
 * @author Huanyu Mark
 */
@Slf4j
@RequiredArgsConstructor
public class HSystemProtocolV1AofSystemPersistService extends HSystemProtocolV1SystemPersistService {

    private final AofConfig aofConfig;

    private final ObjectProvider<AofFlusher> aofProvider;

    private final ObjectProvider<Database> databaseProvider;

    @Override
    protected Path findSystemInfoFile() throws IOException {
        assert aofConfig.getRootPath() != null;
        return Path.of(aofConfig.getRootPath().toString(), aofConfig.getSystemInfoFileName());
    }

    @Override
    protected DatabaseInfos scanDatabaseInfo(File dbDir) throws IOException {
        AofFlusher aofFlusher = aofProvider.getObject(dbDir.toPath());
        if (!(aofFlusher instanceof Aof aof)) {
            throw new UnsupportedOperationException();
        }
        if (aof.getDatabase() == null) {
            return aof.preload(databaseProvider).getInfos();
        }
        return aof.getDatabase().getInfos();
    }

    @Override
    protected @NotNull Lazy<Database> createDatabaseLoader(File dbDir) throws IOException {
        AofFlusher aofFlusher = aofProvider.getObject(dbDir.toPath());
        if (!(aofFlusher instanceof Aof aof)) {
            throw new UnsupportedOperationException();
        }
        if (aof.getDatabase() == null) {
            aof.preload(databaseProvider);
        }
        if (Aof.getAofConfig().isLazyLoad()) {
            return AtomLazy.of(() -> {
                try {
                    aof.loadData();
                } catch (IOException e) {
                    log.error("load aof error", e);
                }
                return aof.getDatabase();
            });
        }
        try {
            aof.loadData();
        } catch (IOException e) {
            log.error("load aof error", e);
        }
        return Lazy.of(aof.getDatabase());
    }
}
