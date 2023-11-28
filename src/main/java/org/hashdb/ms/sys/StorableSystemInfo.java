package org.hashdb.ms.sys;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.PlainPair;
import org.hashdb.ms.persistent.PersistentService;
import org.hashdb.ms.util.AtomLazy;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static final Lazy<PersistentService> persistentService = Lazy.of(() -> HashDBMSApp.ctx().getBean(PersistentService.class));

    static {
        if (log.isTraceEnabled()) {
            log.info("serialVersionUID: {}", serialVersionUID);
        }
    }

    @NotNull
    public SystemInfo restore() {
        var nameDbMap = databaseIdMap.entrySet().parallelStream().map(entry -> {
            var dbName = entry.getValue();
            return new PlainPair<>(dbName, AtomLazy.of(() -> {
                Database db = persistentService.get().scanDatabase(dbName);
                db.startDaemon().join();
                return db;
            }));
        }).collect(Collectors.toMap(PlainPair::key, PlainPair::value));
        var idDbMap = databaseIdMap.entrySet().parallelStream().map(entry -> {
            var dbName = entry.getValue();
            var lazyDb = nameDbMap.get(dbName);
            return new PlainPair<>(entry.getKey(), lazyDb);
        }).collect(Collectors.toMap(PlainPair::key, PlainPair::value));
        return new SystemInfo(nameDbMap, idDbMap);
    }
}
