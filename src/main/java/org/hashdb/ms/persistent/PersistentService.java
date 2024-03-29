package org.hashdb.ms.persistent;

import org.hashdb.ms.compiler.CompileStream;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.DatabaseInfos;
import org.hashdb.ms.manager.SystemInfo;
import org.hashdb.ms.net.exception.NotFoundDatabaseException;
import org.hashdb.ms.net.nio.ClusterGroup;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Date: 2023/11/21 3:01
 * 持久化服务, 保存数据库，或者恢复数据库
 *
 * @author Huanyu Mark
 */
public interface PersistentService {
    boolean persist(Database database);

    boolean persist(SystemInfo systemInfo);

    @Deprecated
    boolean persist(ClusterGroup config);

    boolean persist(CompileStream<?> stream);

    @Deprecated
    List<Database> scanDatabases();

    List<DatabaseInfos> scanDatabaseInfos();

    SystemInfo scanSystemInfo();

    DatabaseInfos scanDatabaseInfo(String name) throws NotFoundDatabaseException;

    Database scanDatabase(String name);

    boolean deleteDatabase(String name);

    @NotNull
    @Deprecated
    ClusterGroup scanReplicationConfig();
}
