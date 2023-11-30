package org.hashdb.ms.persistent;

import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.DatabaseInfos;
import org.hashdb.ms.exception.NotFoundDatabaseException;
import org.hashdb.ms.sys.SystemInfo;

import java.util.List;

/**
 * Date: 2023/11/21 3:13
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class JsonPersistentService implements PersistentService {
    @Override
    public boolean persist(Database database) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean persist(SystemInfo systemInfo) {
//        JacksonSerializer.stringfy()
        return true;
    }

    @Deprecated
    public List<Database> scanDatabases() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DatabaseInfos> scanDatabaseInfos() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DatabaseInfos scanDatabaseInfo(String name) throws NotFoundDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SystemInfo scanSystemInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Database scanDatabase(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean deleteDatabase(String name) {
        throw new UnsupportedOperationException();
    }
}
