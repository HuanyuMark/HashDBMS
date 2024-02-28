package org.hashdb.ms.compiler.keyword.ctx.sys;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hashdb.ms.compiler.SystemCompileStream;
import org.hashdb.ms.compiler.keyword.CompilerNode;
import org.hashdb.ms.compiler.keyword.SystemKeyword;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.manager.DBSystem;
import org.hashdb.ms.net.ConnectionSession;
import org.hashdb.ms.support.StaticAutowired;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2023/11/30 1:06
 *
 * @author Huanyu Mark
 */
public abstract class SystemCompileCtx<R> implements CompilerNode {

    @StaticAutowired
    private static DBSystem SYSTEM;

    protected static DBSystem system() {
        return SYSTEM;
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

    abstract public OpsTask<R> executor(ConnectionSession session);

    @Override
    public abstract SystemKeyword name();
}
