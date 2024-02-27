package org.hashdb.ms.manager;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.aspect.methodAccess.DisposableCall;
import org.hashdb.ms.config.ClusterGroupConfig;
import org.hashdb.ms.config.DBManageConfig;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.net.exception.DatabaseClashException;
import org.hashdb.ms.net.exception.NotFoundDatabaseException;
import org.hashdb.ms.persistent.PersistentService;
import org.hashdb.ms.util.AsyncService;
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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Date: 2023/11/21 1:46
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
@Component
public class DBSystem extends BlockingQueueTaskConsumer implements InitializingBean, DisposableBean {
    @Getter
    private SystemInfo systemInfo;

    @Getter
    private ClusterGroupConfig clusterGroupConfig;

    private final PersistentService persistentService;

    private final DBManageConfig manageConfig;

    public DBSystem(PersistentService persistentService, DBManageConfig manageConfig) {
        this.persistentService = persistentService;
        this.manageConfig = manageConfig;
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

//    public void addDatabase(Database db) throws DatabaseClashException {
//        Objects.requireNonNull(db);
//        if (this.systemInfo.getDatabaseNameMap().containsKey(db.getInfos().getName()) ||
//                this.systemInfo.getDatabaseIdMap().containsKey(db.getInfos().getId())) {
//            throw new DatabaseClashException("database '" + db + "' clash other database");
//        }
//        this.systemInfo.addDatabase(db);
//    }

    public @NotNull Lazy<Database> newDatabase(@Nullable Integer id, @NotNull String name) {
        return newDatabase(id, name, db -> {
        });
    }

    public @NotNull Lazy<Database> newDatabase(@Nullable Integer id, @NotNull String name, Consumer<Database> initializer)
            throws DatabaseClashException {
        Objects.requireNonNull(name);
        Objects.requireNonNull(initializer);
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
                DatabaseClashException clashWithId = new DatabaseClashException("fail to create database. redundancy database id");
                clashWithId.clashWith("id");
                throw clashWithId;
            }
        }
        if (this.systemInfo.getDatabaseNameMap().containsKey(name)) {
            DatabaseClashException clashWithName = new DatabaseClashException("fail to create database. redundancy database name");
            clashWithName.clashWith("name");
            throw clashWithName;
        }
        var newDb = new Database(id, name, new Date());
        initializer.accept(newDb);
        var lazy = Lazy.of(() -> {
            newDb.startDaemon().join();
            return newDb;
        });
        this.systemInfo.addDatabase(newDb.getInfos(), lazy);
        persistentService.persist(this.systemInfo);
        persistentService.persist(newDb);
        return lazy;
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
            throw NotFoundDatabaseException.of(STR."id: '\{id}'");
        }
        return lazy.get();
    }

    @DisposableCall
    private void setSystemInfo(@NotNull SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
        systemInfo.setSystem(this);
    }

    /**
     * 初始化数据库
     */
    @Override
    public void afterPropertiesSet() {
        // 扫描 系统信息,
        setSystemInfo(persistentService.scanSystemInfo());
        initSystemInternalDB();
    }

    public void initSystemInternalDB() {
        initUserDB();
    }

    private static Map<String, String> newUserEntity(String username, String password) {
        @SuppressWarnings("unchecked")
        var userEntity = (Map<String, String>) DataType.MAP.reflect().create();
        userEntity.put(username, password);
        return userEntity;
    }

    public void initUserDB() {
        try {
            newDatabase(1, "user", userDb -> {
                // add default users
                for (var initUser : manageConfig.getInitUsers()) {
                    userDb.set("hash", newUserEntity(initUser.username(), initUser.password()), null, null);
                }
            });
        } catch (DatabaseClashException e) {
            // check if user db is valid
            if (e.getClashKeys().contains("id")) {
                var db = getDatabase(1);
                if (!"user".equals(db.getInfos().getName())) {
                    var clashWithName = new DatabaseClashException();
                    log.error(STR."check system internal db 'user' failed. the id of 'user' db should be 1 and the name of db which itself id is 1 is expect to 'user' but found '\{db.getInfos().getName()}'");
                    clashWithName.clashWith("name");
                    throw new DBSystemException(clashWithName);
                }
                return;
            }
            if (e.getClashKeys().contains("name")) {
                var db = getDatabase("user");
                if (1 != db.getInfos().getId()) {
                    var clashWithId = new DatabaseClashException();
                    log.error(STR."check system internal db 'user' failed. the id of 'user' db which itself name is 'user' should be 1 but found '\{db.getInfos().getId()}'");
                    clashWithId.clashWith("id");
                    throw new DBSystemException(clashWithId);
                }
            }
            // user database is exited and valid
        }
    }

    /**
     * 保存系统信息, 数据库信息, 数据库数据
     */
    @Override
    public void destroy() {
        log.info("start closing System ...");
        var dbmsCostTimeCounter = TimeCounter.start();
        // 保存系统信息
        persistentService.persist(systemInfo);
        if (log.isTraceEnabled()) {
            log.trace("system info storing success");
            log.info("storing database ...");
        }
        // 保存数据库数据
        systemInfo.getDatabaseInfosMap().values().forEach(lazyDb -> {
            if (!lazyDb.isResolved()) {
                return;
            }
            var db = lazyDb.get();
            var reportTimeout = AsyncService.setTimeout(() -> {
                log.warn("Timed out waiting for the database {} to complete all tasks", db);
            }, 10_000);
            db.close();
            reportTimeout.cancel(true);
            persistentService.persist(db);
            if (log.isTraceEnabled()) {
                log.trace("db '{}' storing success", db);
            }
        });
        String forkJoinPoolClosingMsg;
        if (ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS)) {
            forkJoinPoolClosingMsg = "gracefully";
        } else {
            forkJoinPoolClosingMsg = STR."rudely. remaining task \{ForkJoinPool.commonPool().getQueuedTaskCount()}";
        }
        log.info("System closed {}, cost {} ms", forkJoinPoolClosingMsg, dbmsCostTimeCounter.stop());
    }
}
