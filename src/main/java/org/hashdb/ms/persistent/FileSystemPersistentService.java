package org.hashdb.ms.persistent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.HdbConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.DBFileAccessFailedException;
import org.hashdb.ms.exception.NotFoundDatabaseException;

import java.io.File;
import java.nio.file.Paths;

/**
 * Date: 2023/11/21 12:42
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public abstract class FileSystemPersistentService implements PersistentService {

    protected final HdbConfig HDBConfig;

    protected File getHDBFileDir(Database database) {
        return getHDBFileDir(database.getInfos().getName());
    }

    protected File getHDBFileDir(String database) {
        // TODO: 2023/12/26 把hdb的读写写完, 现在为了交差, 先这样写了
        return getDBFileDir(database, "");
//        return getDBFileDir(database, "hdb");
    }

    private File getDBFileDir(String database, String persistentFileCategory) {
        String databaseDirPath = Paths.get(HDBConfig.getRootDir().getAbsolutePath(), database, persistentFileCategory).toString();
        log.info("databaseDirPath: {}", databaseDirPath);
        return FileUtils.prepareDir(
                databaseDirPath,
                (dbDir) -> new DBFileAccessFailedException("can`t access db file dir: " + dbDir.getAbsolutePath())
        );
    }

    protected File getAofFileDir(String database) {
        return getDBFileDir(database, "aof");
    }

    protected File getSystemInfoFile() {
        File dbFileRootDir = HDBConfig.getRootDir();
        return new File(dbFileRootDir, HDBConfig.getSystemInfoFileName());
    }

    protected File getPersistentConfigFile() {
        File dbFileRootDir = HDBConfig.getRootDir();
        return new File(dbFileRootDir, HDBConfig.getReplicationConfigFileName());
    }

    /**
     * 删除
     *
     * @param name 数据库名
     * @return 是否删除成功
     */
    @Override
    public synchronized boolean deleteDatabase(String name) {
        File dbFileDir = new File(HDBConfig.getRootDir(), name);
        if (!dbFileDir.exists()) {
            throw new NotFoundDatabaseException("can not delete database. cause: not found database '" + name + "'");
        }
        if (dbFileDir.delete()) {
            return true;
        }
        throw new DBFileAccessFailedException("can not delete db file dir: " + dbFileDir.getAbsolutePath());
    }
}
