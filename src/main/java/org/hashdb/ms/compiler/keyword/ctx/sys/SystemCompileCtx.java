package org.hashdb.ms.compiler.keyword.ctx.sys;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.compiler.SystemCompileStream;
import org.hashdb.ms.compiler.keyword.CompilerNode;
import org.hashdb.ms.compiler.keyword.SystemKeyword;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.manager.DBSystem;
import org.hashdb.ms.net.ReadonlyConnectionSession;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2023/11/30 1:06
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class SystemCompileCtx<R> implements CompilerNode {

    private static final Lazy<DBSystem> SYSTEM = Lazy.of(() -> HashDBMSApp.ctx().getBean(DBSystem.class));

    protected static DBSystem system() {
        return SYSTEM.get();
    }

    @JsonIgnore
    protected OpsTask<R> result;

    public <S extends SystemCompileCtx<R>> OpsTask<R> compileWithStream(SystemCompileStream stream) {
        result = doCompile(stream);
        return result;
    }

    @Nullable
    public OpsTask<R> getResult() {
        return result;
    }

    abstract OpsTask<R> doCompile(SystemCompileStream stream);

    abstract public OpsTask<R> executor(ReadonlyConnectionSession session);

    @Override
    public abstract SystemKeyword name();
}
