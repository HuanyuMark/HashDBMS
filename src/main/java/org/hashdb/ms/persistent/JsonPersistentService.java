package org.hashdb.ms.persistent;

import org.hashdb.ms.data.Database;

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
        return false;
    }

    @Override
    public List<Database> scanDatabases() {
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
}
