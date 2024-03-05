package org.hashdb.ms.persistent;

import lombok.RequiredArgsConstructor;
import org.hashdb.ms.compiler.CompileStream;
import org.hashdb.ms.config.AofConfig;
import org.hashdb.ms.config.HdbConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.DatabaseInfos;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.StorableHValue;
import org.hashdb.ms.manager.SystemInfo;
import org.hashdb.ms.net.exception.NotFoundDatabaseException;
import org.hashdb.ms.net.nio.ClusterGroup;
import org.hashdb.ms.persistent.hdb.HdbFactory;
import org.hashdb.ms.util.AsyncService;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jol.info.ClassLayout;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: 2024/1/5 19:58
 *
 * @author Huanyu Mark
 */
@RequiredArgsConstructor
public class AofHdbPersistentService implements PersistentService {
    private final AofConfig aofConfig;

    private final HdbConfig hdbConfig;

    @Override
    public boolean persist(Database database) {
        synchronized (database.SAVE_TASK_LOCK) {
            var hdbSaveTask = AsyncService.start(() -> {
                File hdbRootDir = hdbConfig.getRootDir();
                // 将数据库的 HashTable 分块写入多个 {chunkId}.db 文件
                Map<String, StorableHValue<?>> buffer = new HashMap<>();
                int chunkId = 0;
                for (HValue<?> value : database) {
                    buffer.put(value.key(), value.toStorable());
                    long size = ClassLayout.parseInstance(buffer).getLossesTotal();
                    if (size < hdbConfig.getChunkSize()) {
                        continue;
                    }
                    File dbChunkFile = HdbFactory.newHDBChunkFile(hdbRootDir, chunkId++);
                    FileUtils.writeObject(dbChunkFile, buffer);
                    buffer.clear();
                }
                if (!buffer.isEmpty()) {
                    File dbChunkFile = HdbFactory.newHDBChunkFile(hdbRootDir, chunkId);
                    FileUtils.writeObject(dbChunkFile, buffer);
                    buffer.clear();
                }
                // 写入数据库索引文件， 保存数据库的基本信息
                File indexFile = HdbFactory.newIndexFile(hdbRootDir);
//            FileUtils.prepareDir(indexFile, () -> new DBFileAccessFailedException("can`t access index db file '" + indexFile.getAbsolutePath() + "'"));
                DatabaseInfos dbInfos = database.getInfos();
                dbInfos.setLastSaveTime(new Date());
                return FileUtils.writeObject(indexFile, dbInfos);
            });
            var aofSaveTask = AsyncService.start(() -> {
                if (!aofConfig.isEnabled()) {
                    return true;
                }

                File aofRootDir = aofConfig.getRootDir();

                return true;
            });
            return hdbSaveTask.join() && aofSaveTask.join();
        }
    }

    @Override
    public boolean persist(SystemInfo systemInfo) {
        return false;
    }

    @Override
    public boolean persist(ClusterGroup config) {
        return false;
    }

    @Override
    public boolean persist(CompileStream<?> stream) {
        return false;
    }

    @Override
    public List<Database> scanDatabases() {
        return null;
    }

    @Override
    public List<DatabaseInfos> scanDatabaseInfos() {
        return null;
    }

    @Override
    public SystemInfo scanSystemInfo() {
        return null;
    }

    @Override
    public DatabaseInfos scanDatabaseInfo(String name) throws NotFoundDatabaseException {
        return null;
    }

    @Override
    public Database scanDatabase(String name) {
        return null;
    }

    @Override
    public boolean deleteDatabase(String name) {
        return false;
    }

    @Override
    public @NotNull ClusterGroup scanReplicationConfig() {
        return null;
    }
}
