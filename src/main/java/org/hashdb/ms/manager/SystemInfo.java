package org.hashdb.ms.manager;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.DatabaseInfos;
import org.hashdb.ms.data.SimplePair;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.net.exception.DatabaseInUseException;
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
 * @author Huanyu Mark
 */
@Slf4j
@Getter
public class SystemInfo {
    private final Map<String, Lazy<Database>> databaseNameMap = new HashMap<>();

    private final TreeMap<Integer, Lazy<Database>> databaseIdMap = new TreeMap<>();

    private final Map<DatabaseInfos, Lazy<Database>> databaseInfosMap = new HashMap<>();

    private final Map<Lazy<Database>, DatabaseInfos> navigableDbInfosMap = new HashMap<>();

    public final Object SAVE_LOCK = new Object();

    private DBSystem system;

    public void setSystem(DBSystem system) {
        if (this.system != null) {
            throw new RuntimeException();
        }
        this.system = system;
    }

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
            throw new DBSystemException();
        }
        this.databaseNameMap.putAll(databaseNameMap);
        this.databaseIdMap.putAll(databaseIdMap);
        this.databaseInfosMap.putAll(databaseInfosMap);
        databaseInfosMap.forEach((key, value) -> {
            navigableDbInfosMap.put(value, key);
        });
    }

    void addDatabase(@NotNull DatabaseInfos infos, @NotNull Lazy<Database> database) {
        databaseNameMap.put(infos.getName(), database);
        databaseIdMap.put(infos.getId(), database);
        databaseInfosMap.put(infos, database);
        navigableDbInfosMap.put(database, infos);
    }

    Lazy<Database> deleteDatabase(@NotNull Database database) {
        if (database.getUsingCount() != 0) {
            throw new DatabaseInUseException(database + " in use");
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
            if (lazy.isResolved()) {
                dbName = lazy.get().getInfos().getName();
            } else {
                dbName = navigableDbInfosMap.get(lazy).getName();
            }
            return new SimplePair<>(entry.getKey(), dbName);
        }).collect(Collectors.toMap(SimplePair::key, SimplePair::value));
        return new StorableSystemInfo(idNameMap);
    }
}
