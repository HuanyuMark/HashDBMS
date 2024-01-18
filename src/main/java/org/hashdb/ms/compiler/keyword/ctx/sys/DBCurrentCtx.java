package org.hashdb.ms.compiler.keyword.ctx.sys;

import org.hashdb.ms.compiler.SystemCompileStream;
import org.hashdb.ms.compiler.keyword.SystemKeyword;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.DatabaseInfos;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.net.ConnectionSession;

/**
 * Date: 2024/1/3 10:53
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class DBCurrentCtx extends SystemCompileCtx<DatabaseInfos> {

    @Override
    OpsTask<DatabaseInfos> doCompile(SystemCompileStream stream) {
        return executor(stream.session());
    }

    @Override
    public OpsTask<DatabaseInfos> executor(ConnectionSession session) {
        return OpsTask.of(() -> {
            Database database = session.getDatabase();
            if (database == null) {
                return DatabaseInfos.NULL;
            }
            return database.getInfos();
        });
    }

    @Override
    public SystemKeyword name() {
        return SystemKeyword.DBCURRENT;
    }
}
