package org.hashdb.ms.compiler;

import org.hashdb.ms.compiler.keyword.ctx.sys.SystemCompileCtx;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.DBExternalException;
import org.hashdb.ms.net.ConnectionSession;

/**
 * Date: 2023/11/30 14:10
 * 每一个会话都需要有一个 SystemCompileStreamFactory 来生产 编译流
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class CompileStreamFactory {
    private final ConnectionSession session;

    public CompileStreamFactory(ConnectionSession session) {
        this.session = session;
    }

    public static CompileStreamFactory create(ConnectionSession session) {
        return new CompileStreamFactory(session);
    }

    public String run(String command) {
        var compileStream = new SystemCompileStream(session, command);
        var execRes = compileStream.submit();
        if(execRes == null) {
            var db = session.getDatabase();
            if (db == null) {
                throw new DBExternalException("No database selected");
            }
            var supplierCompileStream = new SupplierCompileStream(db, compileStream.tokens, null, false);
            return supplierCompileStream.submit();
        }
        return execRes;
    }
}
