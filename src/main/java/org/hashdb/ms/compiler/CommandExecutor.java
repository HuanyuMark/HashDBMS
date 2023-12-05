package org.hashdb.ms.compiler;

import org.hashdb.ms.exception.DBClientException;
import org.hashdb.ms.net.ConnectionSession;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Date: 2023/11/30 14:10
 * 每一个会话都需要有一个 SystemCompileStreamFactory 来生产 编译流
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class CommandExecutor {
    private final ConnectionSession session;

    protected CommandExecutor(ConnectionSession session) {
        this.session = session;
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull CommandExecutor create(ConnectionSession session) {
        return new CommandExecutor(session);
    }

    public String run(String command) {
        var compileStream = new SystemCompileStream(session, command);
        var execRes = compileStream.submit();
        if (execRes != null) {
            return execRes;
        }
        var db = session.getDatabase();
        if (db == null) {
            throw new DBClientException("No database selected");
        }
        var supplierCompileStream = new SupplierCompileStream(db, compileStream.tokens, null, false);
        return supplierCompileStream.submit();
    }
}
