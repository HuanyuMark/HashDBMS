package org.hashdb.ms.sys;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.DBFileConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.PlainPair;
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
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public record StorableSystemInfo(
        Map<Integer, String> databaseIdMap
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 429452356L;

    static {
        if (log.isTraceEnabled()) {
            log.info("serialVersionUID: {}", serialVersionUID);
        }
    }

    @NotNull
    public SystemInfo restoreBy(DBFileConfig dbFileConfig, PersistentService persistentService) {
        var nameDbMap = databaseIdMap.entrySet().parallelStream().map(entry -> {
            var dbName = entry.getValue();
            AtomLazy<Database> atomLazy;
            if (dbFileConfig.isLazyLoad()) {
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
            return new PlainPair<>(dbName,atomLazy);
        }).collect(Collectors.toMap(PlainPair::key, PlainPair::value));
        var idDbMap = databaseIdMap.entrySet().parallelStream().map(entry -> {
            var dbName = entry.getValue();
            var lazyDb = nameDbMap.get(dbName);
            return new PlainPair<>(entry.getKey(), lazyDb);
        }).collect(Collectors.toMap(PlainPair::key, PlainPair::value));
        var dbInfosMap = persistentService.scanDatabaseInfos().stream()
                .map(info -> new PlainPair<>(info, nameDbMap.get(info.getName())))
                .collect(Collectors.toMap(PlainPair::key, PlainPair::value));
        return new SystemInfo(nameDbMap, idDbMap, dbInfosMap);
    }
}
