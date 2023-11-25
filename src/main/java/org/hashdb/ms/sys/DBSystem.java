package org.hashdb.ms.sys;

import lombok.RequiredArgsConstructor;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.NotFoundDatabaseException;
import org.hashdb.ms.persistent.PersistentService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Date: 2023/11/21 1:46
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Component
@RequiredArgsConstructor
public class DBSystem implements InitializingBean {
    private final Map<String, Database> databases = new HashMap<>();
    private final PersistentService persistentService;
    public Set<Map.Entry<String, Database>> getDatabases() {
        return databases.entrySet();
    }
    public void delDatabases(String dbName) {
        Database database = this.databases.remove(dbName);
        if(database == null) {
            throw NotFoundDatabaseException.of(dbName);
        }
        database.clear();
        persistentService.deleteDatabase(dbName);
    }

    public void addDatabase(String name, Database database) {
        this.databases.put(name, database);
    }
    public Database getDatabase(String name) {
        return this.databases.get(name);
    }
    @Override
    public void afterPropertiesSet() throws Exception {
        // 扫描 数据库文件， 恢复数据库
        persistentService.scanDatabases().forEach(db->databases.put(db.getInfos().getName(),db));
    }
}
