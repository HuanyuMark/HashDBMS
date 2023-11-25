package org.hashdb.ms.compiler;

import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.OpsTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Date: 2023/11/24 16:03
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class DatabaseCommandCompiler {
    private final Database database;

    public DatabaseCommandCompiler(Database database) {
        Objects.requireNonNull(database);
        this.database = database;
    }
    public OpsTask<?> compile(@NotNull String command) {
        return SupplierCompileStream.open(database, command).compile().compileResult();
    }
}
