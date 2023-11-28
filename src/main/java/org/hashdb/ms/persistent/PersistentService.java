package org.hashdb.ms.persistent;

import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.DatabaseInfos;
import org.hashdb.ms.exception.NotFoundDatabaseException;
import org.hashdb.ms.sys.SystemInfo;

import java.util.List;

/**
 * Date: 2023/11/21 3:01
 * 持久化服务, 保存数据库，或者恢复数据库
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface PersistentService {
    boolean persist(Database database);

    boolean persist(SystemInfo systemInfo);
    @Deprecated
    List<Database> scanDatabases();
    List<DatabaseInfos> scanDatabaseInfos();

    SystemInfo scanSystemInfo();

    DatabaseInfos scanDatabaseInfo(String name) throws NotFoundDatabaseException;

    Database scanDatabase(String name);

    boolean deleteDatabase(String name);
}
