package org.hashdb.ms.compiler.keyword.ctx.sys;

import org.hashdb.ms.compiler.SystemCompileStream;
import org.hashdb.ms.compiler.exception.CommandInterpretException;
import org.hashdb.ms.compiler.keyword.SystemKeyword;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.net.ConnectionSession;
import org.hashdb.ms.net.bio.BIOConnectionSession;
import org.hashdb.ms.net.exception.NotFoundDatabaseException;

/**
 * Date: 2023/11/30 0:48
 *
 * @author Huanyu Mark
 */
public class DBUseCtx extends SystemCompileCtx<Boolean> {
    protected Integer dbId;
    protected String dbName;

    @Override
    public SystemKeyword name() {
        return SystemKeyword.DBUSE;
    }

    private static final OpsTask<Boolean> PLACE_HOLDER = OpsTask.of(() -> true);

    @Override
    protected OpsTask<Boolean> doCompile(SystemCompileStream stream) {
        stream.toWrite();
        while (true) {
            String token;
            try {
                token = stream.token();
            } catch (ArrayIndexOutOfBoundsException e) {
                break;
            }
            if (dbId != null) {
                throw new CommandInterpretException("redundancy param: 'database id'");
            }
            try {
                dbId = Integer.valueOf(token);
                stream.next();
                continue;
            } catch (NumberFormatException ignore) {
            }
            if (dbId != null) {
                throw new CommandInterpretException("redundancy param: 'database id'");
            }
            dbName = token;
            stream.next();
        }
        if (dbId == null) {
            if (dbName == null) {
                throw new CommandInterpretException("keyword '" + name() + "' require param: database id(Integer) or name(String)");
            }
            if (stream.session() instanceof BIOConnectionSession session) {
                session.setDatabase(system().getDatabase(dbName));
                return PLACE_HOLDER;
            }
            throw new DBSystemException("unexpected ops");
        }
        if (dbName != null) {
            if (!system().getDatabaseNameMap().containsKey(dbName)) {
                throw NotFoundDatabaseException.of("{\"id\":" + dbId + ",\"name\":\"" + dbName + "\"}");
            }
        }
        if (stream.session() instanceof BIOConnectionSession session) {
            session.setDatabase(system().getDatabase(dbId));
            return PLACE_HOLDER;
        }
        throw new DBSystemException("unexpected ops");
    }

    @Override
    public OpsTask<Boolean> executor(ConnectionSession session) {
        return PLACE_HOLDER;
    }
}
