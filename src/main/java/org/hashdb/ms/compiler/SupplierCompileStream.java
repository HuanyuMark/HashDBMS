package org.hashdb.ms.compiler;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.CommandCompileException;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Date: 2023/11/24 15:58
 * 编译流, 流式编译一整条命令
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public class SupplierCompileStream extends DatabaseCompileStream {

    private SupplierCompileStream(Database database, String command) {
        super(database, command);
    }

    SupplierCompileStream(Database database, String[] tokens, DatabaseCompileStream fatherStream) {
        super(database, tokens, fatherStream);
    }

    public static @NotNull SupplierCompileStream open(Database db, @NotNull String command) {
        Objects.requireNonNull(db);
        return new SupplierCompileStream(db, command);
    }

    @Override
    public SupplierCtx compile() {
        var commandContext = SupplierKeyword.createCtx(token());
        if (commandContext == null) {
            throw new CommandCompileException("unknown keyword: '" + token() + "'");
        }
        next();
        commandContext.compileWithStream(this);
        return commandContext;
    }
}
