package org.hashdb.ms.manager;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.HdbConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.SimplePair;
import org.hashdb.ms.persistent.PersistentService;
import org.hashdb.ms.util.AtomLazy;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Date: 2023/11/27 23:34
 *
 * @author Huanyu Mark
 */
@Slf4j
public record StorableSystemInfo(
        Map<Integer, String> databaseIdMap
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 429452356L;

    @NotNull
    public SystemInfo restoreBy(HdbConfig HDBConfig, PersistentService persistentService) {
        var nameDbMap = databaseIdMap.entrySet().parallelStream().map(entry -> {
            var dbName = entry.getValue();
            AtomLazy<Database> atomLazy;
            if (HDBConfig.isLazyLoad()) {
                atomLazy = AtomLazy.of(() -> {
                    Database db = persistentService.scanDatabase(dbName);
                    db.startDaemon().join();
                    return db;
                });
            } else {
                Database db = persistentService.scanDatabase(dbName);
                atomLazy = AtomLazy.of(() -> {
                    db.startDaemon().join();
                    return db;
                });
            }
            return new SimplePair<>(dbName, atomLazy);
        }).collect(Collectors.toMap(SimplePair::key, SimplePair::value));
        var idDbMap = databaseIdMap.entrySet().parallelStream().map(entry -> {
            var dbName = entry.getValue();
            var lazyDb = nameDbMap.get(dbName);
            return new SimplePair<>(entry.getKey(), lazyDb);
        }).collect(Collectors.toMap(SimplePair::key, SimplePair::value));
        var dbInfosMap = persistentService.scanDatabaseInfos().stream()
                .map(info -> new SimplePair<>(info, nameDbMap.get(info.getName())))
                .collect(Collectors.toMap(SimplePair::key, SimplePair::value));
        return new SystemInfo(nameDbMap, idDbMap, dbInfosMap);
    }
}
