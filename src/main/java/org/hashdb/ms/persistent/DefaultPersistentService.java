package org.hashdb.ms.persistent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.HdbConfig;
import org.hashdb.ms.config.ReplicationConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.DatabaseInfos;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.StorableHValue;
import org.hashdb.ms.exception.DBClientException;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.exception.NotFoundDatabaseException;
import org.hashdb.ms.sys.StorableSystemInfo;
import org.hashdb.ms.sys.SystemInfo;
import org.hashdb.ms.util.AtomLazy;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jol.info.ClassLayout;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

/**
 * Date: 2023/11/21 3:02
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
@Component
public class DefaultPersistentService extends FileSystemPersistentService {

    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();
//    private DBSystem dbSystem;

    private final StandardEnvironment environment;

    public DefaultPersistentService(HdbConfig hdbConfig, StandardEnvironment environment) {
        super(hdbConfig);
        this.environment = environment;
    }

    /**
     * 缺陷：该操作(文件操作)不具有原子性，抛出异常后， 可能会有文件碎片残留
     *
     * @param database 数据库
     */
    @Override
    public boolean persist(Database database) {
        Objects.requireNonNull(database);
        synchronized (database.SAVE_TASK_LOCK) {
            File dbFileDir = getHDBFileDir(database);
            // 将数据库的 HashTable 分块写入多个 {chunkId}.db 文件
            Map<String, StorableHValue<?>> buffer = new HashMap<>();
            int chunkId = 0;
            for (HValue<?> value : database) {
                buffer.put(value.key(), value.toStorable());
                long size = ClassLayout.parseInstance(buffer).getLossesTotal();
                if (size < HDBConfig.getChunkSize()) {
                    continue;
                }
                File dbChunkFile = DBFileFactory.newDBChunkFile(dbFileDir, chunkId++);
                writeObject(dbChunkFile, buffer);
                buffer.clear();
            }
            if (!buffer.isEmpty()) {
                File dbChunkFile = DBFileFactory.newDBChunkFile(dbFileDir, chunkId);
                writeObject(dbChunkFile, buffer);
                buffer.clear();
            }
            // 写入数据库索引文件， 保存数据库的基本信息
            File indexFile = DBFileFactory.newIndexFile(dbFileDir);
//            FileUtils.prepareDir(indexFile, () -> new DBFileAccessFailedException("can`t access index db file '" + indexFile.getAbsolutePath() + "'"));
            DatabaseInfos dbInfos = database.getInfos();
            dbInfos.setLastSaveTime(new Date());
            return writeObject(indexFile, dbInfos);
        }
    }

    @Override
    public boolean persist(SystemInfo systemInfo) {
        Objects.requireNonNull(systemInfo);
        synchronized (systemInfo.SAVE_LOCK) {
            File systemInfoFile = getSystemInfoFile();
            return writeObject(systemInfoFile, systemInfo.toStorableSystemInfo());
        }
    }

    @Override
    public boolean persist(ReplicationConfig config) {
        try {
            String toSave = YAML_MAPPER.writeValueAsString(config)
                    .replaceFirst("---\n", "---\n#Generated By Hash DB\n");
            try (FileWriter writer = new FileWriter(getPersistentConfigFile())) {
                writer.write(toSave);
            } catch (IOException e) {
                log.error("can not generate replication config file ", e);
                throw new DBSystemException(e);
            }
            return true;
        } catch (JsonProcessingException e) {
            throw new DBSystemException(e);
        }
    }

    /**
     * 因为引入了 {@link HdbConfig#isLazyLoad()} 配置项, DBMS 已支持
     * 用户设置是否使用懒加载来加载数据库, 但是该方法只能支持  非懒加载(即立即加载) 的
     * 方式, 直接读取全部数据库, 所以该方法已废弃
     *
     * @return 读取数据库根文件目录下所有数据库
     */
    @Override
    @Deprecated
    public List<Database> scanDatabases() {
        return Arrays.stream(
                        Objects.requireNonNullElse(
                                HDBConfig.getRootDir()
                                        .listFiles(file -> !HDBConfig.getSystemInfoFileName().equals(file.getName())
                                                && file.isDirectory() && file.canRead()),
                                new File[0]
                        )
                )
                .parallel()
                .map(DefaultPersistentService::scanDatabase)
                .toList();
    }

    @Override
    public List<DatabaseInfos> scanDatabaseInfos() {
        return Arrays.stream(
                        Objects.requireNonNullElse(
                                HDBConfig.getRootDir()
                                        .listFiles(file -> !HDBConfig.getSystemInfoFileName().equals(file.getName())
                                                && file.isDirectory() && file.canRead()),
                                new File[0]
                        )
                )
                .parallel()
                .map(DefaultPersistentService::scanDatabaseInfo)
                .toList();
    }

    @Override
    public DatabaseInfos scanDatabaseInfo(String name) throws NotFoundDatabaseException {
        File dbFileDir = getHDBFileDir(name);
        if (!dbFileDir.exists()) {
            throw NotFoundDatabaseException.of(name);
        }
        if (!dbFileDir.isDirectory()) {
            throw new DBClientException("db file '" + dbFileDir.getAbsolutePath() + "' is not a directory");
        }
        return scanDatabaseInfo(dbFileDir);
    }

    private static DatabaseInfos scanDatabaseInfo(File dbFileDir) {
        // 读取数据库索引文件，获取数据库基本信息
        File indexFile = DBFileFactory.loadIndexFile(dbFileDir);
        return (DatabaseInfos) readObject(indexFile);
    }

    @Override
    public Database scanDatabase(String name) throws NotFoundDatabaseException {
        File dbFileDir = new File(HDBConfig.getRootDir(), name);
        if (!dbFileDir.exists()) {
            throw NotFoundDatabaseException.of(name);
        }
        if (!dbFileDir.isDirectory()) {
            throw new DBClientException("db file '" + dbFileDir.getAbsolutePath() + "' is not a directory");
        }
        return scanDatabase(dbFileDir);
    }

    @NotNull
    private static Database scanDatabase(File dbFileDir) {
        DatabaseInfos databaseInfos = scanDatabaseInfo(dbFileDir);

        // 读取数据库的 HashTable 分块
        File[] dbChunkFile = DBFileFactory.loadDBChunkFile(dbFileDir);
        Map<String, StorableHValue<?>> initEntries = new HashMap<>();
        Arrays.stream(dbChunkFile).parallel().forEach(file -> {
            @SuppressWarnings("unchecked")
            Map<String, StorableHValue<?>> chunk = (Map<String, StorableHValue<?>>) readObject(file);
            initEntries.putAll(chunk);
        });

        return new Database(databaseInfos, initEntries);
    }

    /**
     * 没有 系统描述信息, 就读取数据库文件根路径, 扫描所有数据库的描述信息
     * 然后汇总为系统描述信息, 保存为新系统描述文件, 然后返回
     *
     * @return 系统描述信息
     */
    @Override
    public SystemInfo scanSystemInfo() {
        File[] files = HDBConfig.getRootDir().listFiles(file -> HDBConfig.getSystemInfoFileName().equals(file.getName()));
        if (files != null) {
            if (files.length > 1) {
                throw new DBClientException("system info file is not unique");
            }
            if (files.length == 1) {
                StorableSystemInfo storableSystemInfo = (StorableSystemInfo) readObject(files[0]);
                return storableSystemInfo.restoreBy(HDBConfig, this);
            }
            if (log.isWarnEnabled()) {
                log.warn("system info file not found. create by scanning db file root directory '" + HDBConfig.getRootDir() + "'");
            }
        }
        HashMap<String, Lazy<Database>> nameDbMap = new HashMap<>();
        HashMap<Integer, Lazy<Database>> idDbMap = new HashMap<>();
        HashMap<DatabaseInfos, Lazy<Database>> infosDbMap = new HashMap<>();
        var databaseInfos = scanDatabaseInfos();
        databaseInfos.parallelStream().forEach(databaseInfo -> {
            AtomLazy<Database> dbLoder;
            if (HDBConfig.isLazyLoad()) {
                dbLoder = AtomLazy.of(() -> {
                    Database loadedDb = scanDatabase(databaseInfo.getName());
                    loadedDb.startDaemon().join();
                    return loadedDb;
                });
            } else {
                Database db = scanDatabase(databaseInfo.getName());
                dbLoder = AtomLazy.of(() -> {
                    db.startDaemon();
                    return db;
                });
            }
            nameDbMap.put(databaseInfo.getName(), dbLoder);
            idDbMap.put(databaseInfo.getId(), dbLoder);
            infosDbMap.put(databaseInfo, dbLoder);
        });
        var systemInfo = new SystemInfo(nameDbMap, idDbMap, infosDbMap);
        persist(systemInfo);
        return systemInfo;
    }

    @Override
    public @NotNull ReplicationConfig scanReplicationConfig() {
        File persistentConfigFile = getPersistentConfigFile();
        if (!persistentConfigFile.exists()) {
            ReplicationConfig replicationConfig = new ReplicationConfig();
            persist(replicationConfig);
            return replicationConfig;
        }
        try {
            return YAML_MAPPER.readValue(new FileReader(persistentConfigFile), ReplicationConfig.class);
        } catch (FileNotFoundException e) {
            log.error("unexpected exception ", e);
            throw new DBSystemException(e);
        } catch (IOException e) {
            throw new DBSystemException(e);
        }
    }
}
