package org.hashdb.ms.manager;

import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.DBManageConfig;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.DatabaseInfos;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.net.exception.DatabaseClashException;
import org.hashdb.ms.net.exception.NotFoundDatabaseException;
import org.hashdb.ms.net.nio.ClusterGroup;
import org.hashdb.ms.persistent.PersistentService;
import org.hashdb.ms.persistent.sys.SystemPersistService;
import org.hashdb.ms.util.AsyncService;
import org.hashdb.ms.util.BlockingQueueTaskConsumer;
import org.hashdb.ms.util.Lazy;
import org.hashdb.ms.util.TimeCounter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.IOException;
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
 * @author Huanyu Mark
 */
@Slf4j
@Component
public class DBSystem extends BlockingQueueTaskConsumer implements Closeable {
    @Getter
    private final SystemInfo systemInfo;

    @Getter
    private ClusterGroup clusterGroup;

    @Deprecated
    private final PersistentService persistentService;

    private final SystemPersistService persistService;

    private final DBManageConfig manageConfig;

    private final ObjectProvider<Database> databaseProvider;

    public DBSystem(
            PersistentService persistentService,
            DBManageConfig manageConfig,
            SystemPersistService persistService,
            SystemInfo systemInfo,
            ObjectProvider<Database> databaseProvider
    ) {
        this.systemInfo = systemInfo;
        systemInfo.setSystem(this);
        this.persistentService = persistentService;
        this.manageConfig = manageConfig;
        this.persistService = persistService;
        this.databaseProvider = databaseProvider;
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
        var newDb = databaseProvider.getObject(new DatabaseInfos(id, name, new Date()));
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

    /**
     * 初始化数据库
     */
    @EventListener(ApplicationContext.class)
    public void init(SystemInfo systemInfo) {
        initSystemInternalDB();
    }

    @Override
    public void close() throws IOException {
        persistService.write(systemInfo);
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
     * {@link PreDestroy} 执行的比 {@link org.springframework.beans.factory.DisposableBean}
     * 早, 且不在 {@link Runtime#addShutdownHook} 中的线程中运行, 所以 {@link ForkJoinPool#commonPool()}
     * 不会提前关闭, 这样可以让所有的虚拟线程得以执行完毕
     */
    @PreDestroy
    public void destroy() {
        log.info("start closing System ...");
        var dbmsCostTimeCounter = TimeCounter.start();
        stopConsumeOpsTask();
        // 保存系统信息
        persistentService.persist(systemInfo);
        if (log.isTraceEnabled()) {
            log.trace("system info storing success");
            log.info("storing database ...");
        }
        if (AsyncService.close(3, TimeUnit.SECONDS)) {
            log.warn("some async tasks close timeout");
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
            try {
                db.close();
            } catch (Exception e) {
                log.error(STR."fail to close database \{db}", e);
            }
            reportTimeout.cancel(true);
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
        log.info("System closed {}, cost {} ms", forkJoinPoolClosingMsg, dbmsCostTimeCounter);
    }


}
