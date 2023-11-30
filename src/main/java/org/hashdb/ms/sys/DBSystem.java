package org.hashdb.ms.sys;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.DatabaseClashException;
import org.hashdb.ms.exception.NotFoundDatabaseException;
import org.hashdb.ms.persistent.PersistentService;
import org.hashdb.ms.util.BlockingQueueTaskConsumer;
import org.hashdb.ms.util.Lazy;
import org.hashdb.ms.util.TimeCounter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Date: 2023/11/21 1:46
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
@Component
public final class DBSystem extends BlockingQueueTaskConsumer implements InitializingBean, DisposableBean {
    @Getter
    private SystemInfo systemInfo;
    private final PersistentService persistentService;

    public DBSystem(PersistentService persistentService) {
        this.persistentService = persistentService;
        startConsumeOpsTask();
    }

    public Map<String, Lazy<Database>> getDatabaseNameMap() {
        return systemInfo.getDatabaseNameMap();
    }

    public Map<Integer, Lazy<Database>> getDatabaseIdMap() {
        return systemInfo.getDatabaseIdMap();
    }

    public void delDatabases(Database db) {
        Lazy<Database> dbLazy = this.systemInfo.deleteDatabase(db);
        if (dbLazy == null) {
            throw NotFoundDatabaseException.of(db.getInfos().getName());
        }
        persistentService.deleteDatabase(db.getInfos().getName());
    }

    public void addDatabase(Database db) throws DatabaseClashException {
        Objects.requireNonNull(db);
        if (this.systemInfo.getDatabaseNameMap().containsKey(db.getInfos().getName()) ||
                this.systemInfo.getDatabaseIdMap().containsKey(db.getInfos().getId())) {
            throw new DatabaseClashException("database '" + db + "' clash other database");
        }
        this.systemInfo.addDatabase(db);
    }

    public @NotNull Lazy<Database> newDatabase(@Nullable Integer id, @NotNull String name) {
        Objects.requireNonNull(name);
        if (id == null) {
            Integer maxId;
            try {
                maxId = systemInfo.getDatabaseIdMap().lastKey();
                id = maxId + 1;
            } catch (NoSuchElementException e) {
                id = 1;
            }
        } else {
            if (this.systemInfo.getDatabaseIdMap().containsKey(id)) {
                throw new DatabaseClashException("fail to create database. redundancy database id");
            }
        }
        if(this.systemInfo.getDatabaseNameMap().containsKey(name)) {
            throw new DatabaseClashException("fail to create database. redundancy database name");
        }
        Database newDb = new Database(id, name, new Date());
        Lazy<Database> lazy = Lazy.of(() -> {
            newDb.startDaemon().join();
            return newDb;
        });
        this.systemInfo.addDatabase(newDb);
        persistentService.persist(this.systemInfo);
        persistentService.persist(newDb);
        return lazy;
    }

    public void tryAddDatabase(Database db) {
        try {
            addDatabase(db);
        } catch (DatabaseClashException e) {
            log.warn("database '" + db + "' clash other database");
        }
    }

    public Database getDatabase(String name) throws NotFoundDatabaseException {
        Lazy<Database> lazy = this.systemInfo.getDatabaseNameMap().get(name);
        if (lazy == null) {
            throw NotFoundDatabaseException.of(name);
        }
        return lazy.get();
    }

    public Database getDatabase(Integer id) throws NotFoundDatabaseException {
        Lazy<Database> lazy = this.systemInfo.getDatabaseIdMap().get(id);
        if (lazy == null) {
            throw NotFoundDatabaseException.of("id: '" + id + "'");
        }
        return lazy.get();
    }

    public PersistentService getPersistentService() {
        return persistentService;
    }

    /**
     * 初始化数据库
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 扫描 系统信息,
        systemInfo = persistentService.scanSystemInfo();
    }

    /**
     * 保存系统信息, 数据库信息, 数据库数据
     */
    @Override
    public void destroy() throws Exception {
        log.info("closing DBMS ...");
        var DbmsCostTime = TimeCounter.start();
        persistentService.persist(systemInfo);
        if (log.isTraceEnabled()) {
            log.trace("system info storing success");
        }
        systemInfo.getDatabaseInfosMap().values().forEach(lazyDb -> {
            if (!lazyDb.isCached()) {
                return;
            }
            Database db = lazyDb.get();
            persistentService.persist(db);
            if (log.isTraceEnabled()) {
                log.trace("db '{}' storing success", db);
            }
        });
        log.info("DBMS closed, cost {} ms", DbmsCostTime.stop());
    }
}
