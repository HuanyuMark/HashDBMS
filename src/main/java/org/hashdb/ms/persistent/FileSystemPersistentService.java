package org.hashdb.ms.persistent;

import lombok.RequiredArgsConstructor;
import org.hashdb.ms.config.DBFileConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.DBFileAccessFailedException;
import org.hashdb.ms.exception.NotFoundDatabaseException;

import java.io.File;

/**
 * Date: 2023/11/21 12:42
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@RequiredArgsConstructor
public abstract class FileSystemPersistentService implements PersistentService{

    protected final DBFileConfig dbFileConfig;
    protected File getDBFileDir(Database database) {
        String databaseDirPath = dbFileConfig.getDbFileRootDir().getAbsolutePath() + database.getInfos().getName();
        return FileUtils.prepareDir(
                databaseDirPath,
                (dbDir)->new DBFileAccessFailedException("can`t access db file dir: "+dbDir.getAbsolutePath()));
    }

    /**
     * 删除
     * @param name 数据库名
     * @return 是否删除成功
     */
    @Override
    public boolean deleteDatabase(String name) {
        File dbFileDir = new File(dbFileConfig.getDbFileRootDir(), name);
        dbFileDir.deleteOnExit();
        return true;
    }
}
