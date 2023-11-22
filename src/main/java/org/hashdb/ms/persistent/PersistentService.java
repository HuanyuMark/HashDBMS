package org.hashdb.ms.persistent;

import org.hashdb.ms.data.Database;

import java.util.List;

/**
 * Date: 2023/11/21 3:01
 * 持久化服务, 保存数据库，或者恢复数据库
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface PersistentService {
    boolean persist(Database database);
    List<Database> scanDatabases();
    Database scanDatabase(String name);
    boolean deleteDatabase(String name);
}
