package org.hashdb.ms.sys;

import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.PlainPair;
import org.hashdb.ms.exception.DBInnerException;
import org.hashdb.ms.persistent.PersistentService;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Date: 2023/11/27 15:04
 * DBMS 系统信息
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class SystemInfo  {
    private final Map<String, Lazy<Database>> databaseNameMap = new HashMap<>();

    private final Map<Integer, Lazy<Database>> databaseIdMap = new HashMap<>();

    private final Map<Lazy<Database>, String> navigableMap = new HashMap<>();

    private static final Lazy<PersistentService> persistentService = Lazy.of(() -> HashDBMSApp.ctx().getBean(PersistentService.class));
    public final Object SAVE_LOCK = new Object();

    public SystemInfo() {
    }

    public <L extends Lazy<Database>> SystemInfo(Map<String, L> databaseNameMap, Map<Integer, L> databaseIdMap) {
        Objects.requireNonNull(databaseNameMap);
        Objects.requireNonNull(databaseIdMap);
        if(databaseIdMap.size() != databaseNameMap.size()) {
            throw new DBInnerException();
        }
        this.databaseNameMap.putAll(databaseNameMap);
        this.databaseIdMap.putAll(databaseIdMap);
        databaseNameMap.forEach((key, value)->{
            this.navigableMap.put(value, key);
        });
    }


    Set<Lazy<Database>> allDb(){
        return navigableMap.keySet();
    }

    Set<String> dbNames() {
        return databaseNameMap.keySet();
    }

    Set<Integer> dbIds() {
        return databaseIdMap.keySet();
    }

    Map<String, Lazy<Database>> dbNameMap() {
        return Collections.unmodifiableMap(databaseNameMap);
    }

    void addDatabase(@NotNull Database database) {
        Lazy<Database> databaseLazy = Lazy.of(database);
        databaseNameMap.put(database.getInfos().getName(), databaseLazy);
        databaseIdMap.put(database.getInfos().getId(), databaseLazy);
        navigableMap.put(databaseLazy, database.getInfos().getName());
    }

    Lazy<Database> removeDatabase(@NotNull Database database) {
        var databaseLazy = databaseNameMap.remove(database.getInfos().getName());
        navigableMap.remove(databaseLazy);
        return databaseIdMap.remove(database.getInfos().getId());
    }

    Map<Integer, Lazy<Database>> dbIdMap() {
        return Collections.unmodifiableMap(databaseIdMap);
    }

    public StorableSystemInfo toStorableSystemInfo() {
        Map<Integer, String> idNameMap = databaseIdMap.entrySet().parallelStream().map(entry -> {
            Lazy<Database> lazy = entry.getValue();
            String dbName;
            if (lazy.isCached()) {
                dbName = lazy.get().getInfos().getName();
            } else {
                dbName = persistentService.get().scanDatabaseInfo(navigableMap.get(lazy)).getName();
            }
            return new PlainPair<>(entry.getKey(), dbName);
        }).collect(Collectors.toMap(PlainPair::key, PlainPair::value));
        return new StorableSystemInfo(idNameMap);
    }
}
