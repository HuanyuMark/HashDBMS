package org.hashdb.ms.compiler.keyword.ctx.sys;

import org.hashdb.ms.compiler.SystemCompileStream;
import org.hashdb.ms.compiler.keyword.SystemKeyword;
import org.hashdb.ms.exception.CommandInterpretException;
import org.hashdb.ms.exception.NotFoundDatabaseException;

/**
 * Date: 2023/11/30 0:48
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class DBUseCtx extends SystemCompileCtx<Boolean> {

    protected Integer dbId;
    protected String dbName;

    @Override
    public SystemKeyword name() {
        return SystemKeyword.DBUSE;
    }

    @Override
    protected Boolean doInterpret(SystemCompileStream stream) {
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
            stream.getSession().setDatabase(system().getDatabase(dbName));
            return Boolean.TRUE;
        }
        if (dbName != null) {
            if (!system().getDatabaseNameMap().containsKey(dbName)) {
                throw NotFoundDatabaseException.of("{\"id\":" + dbId + ",\"name\":\"" + dbName + "\"}");
            }
        }
        stream.getSession().setDatabase(system().getDatabase(dbId));
        return Boolean.TRUE;
    }
}
