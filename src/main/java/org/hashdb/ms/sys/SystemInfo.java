package org.hashdb.ms.sys;

import lombok.Getter;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.DatabaseInfos;
import org.hashdb.ms.data.PlainPair;
import org.hashdb.ms.exception.DBInnerException;
import org.hashdb.ms.exception.DatabaseInUseException;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Date: 2023/11/27 15:04
 * DBMS 系统信息
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Getter
public class SystemInfo {
    private final Map<String, Lazy<Database>> databaseNameMap = new HashMap<>();

    private final TreeMap<Integer, Lazy<Database>> databaseIdMap = new TreeMap<>();

    private final Map<DatabaseInfos, Lazy<Database>> databaseInfosMap = new HashMap<>();

    private final Map<Lazy<Database>, DatabaseInfos> navigableDbInfosMap = new HashMap<>();

    private static final Lazy<DBSystem> dbSystem = Lazy.of(() -> HashDBMSApp.ctx().getBean(DBSystem.class));
    public final Object SAVE_LOCK = new Object();

    public SystemInfo() {
    }

    public <L extends Lazy<Database>> SystemInfo(
            Map<String, L> databaseNameMap,
            Map<Integer, L> databaseIdMap,
            Map<DatabaseInfos, L> databaseInfosMap
    ) {
        Objects.requireNonNull(databaseNameMap);
        Objects.requireNonNull(databaseIdMap);
        Objects.requireNonNull(databaseInfosMap);
        if (databaseIdMap.size() != databaseNameMap.size()) {
            throw new DBInnerException();
        }
        this.databaseNameMap.putAll(databaseNameMap);
        this.databaseIdMap.putAll(databaseIdMap);
        this.databaseInfosMap.putAll(databaseInfosMap);
        databaseInfosMap.forEach((key, value) -> {
            navigableDbInfosMap.put(value, key);
        });
    }

    void addDatabase(@NotNull Database database) {
        addDatabase(database.getInfos(), Lazy.of(database));
    }

    void addDatabase(@NotNull DatabaseInfos infos, @NotNull Lazy<Database> database) {
        databaseNameMap.put(infos.getName(), database);
        databaseIdMap.put(infos.getId(), database);
        databaseInfosMap.put(infos,database);
        navigableDbInfosMap.put(database, infos);
    }

    Lazy<Database> deleteDatabase(@NotNull Database database) {
        if(database.getTackUpCount() != 0) {
            throw new DatabaseInUseException(database+" in use");
        } else {
            database.shutdownConsumer();
        }
        var databaseLazy = databaseNameMap.remove(database.getInfos().getName());
        if (databaseLazy == null) {
            return null;
        }
        databaseIdMap.remove(database.getInfos().getId());
        navigableDbInfosMap.remove(databaseLazy);
        databaseInfosMap.remove(database.getInfos());
        return databaseLazy;
    }

    public StorableSystemInfo toStorableSystemInfo() {
        Map<Integer, String> idNameMap = databaseIdMap.entrySet().parallelStream().map(entry -> {
            Lazy<Database> lazy = entry.getValue();
            String dbName;
            if (lazy.isCached()) {
                dbName = lazy.get().getInfos().getName();
            } else {
                dbName = dbSystem.get().getSystemInfo().getNavigableDbInfosMap().get(lazy).getName();
            }
            return new PlainPair<>(entry.getKey(), dbName);
        }).collect(Collectors.toMap(PlainPair::key, PlainPair::value));
        return new StorableSystemInfo(idNameMap);
    }
}
