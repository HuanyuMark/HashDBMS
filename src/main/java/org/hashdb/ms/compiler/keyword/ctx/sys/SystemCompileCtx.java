package org.hashdb.ms.compiler.keyword.ctx.sys;

import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.compiler.SystemCompileStream;
import org.hashdb.ms.compiler.keyword.CompilerNode;
import org.hashdb.ms.compiler.keyword.SystemKeyword;
import org.hashdb.ms.sys.DBSystem;
import org.hashdb.ms.util.Lazy;

/**
 * Date: 2023/11/30 1:06
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class SystemCompileCtx<R> implements CompilerNode {

    private static final Lazy<DBSystem> SYSTEM =  Lazy.of(()-> HashDBMSApp.ctx().getBean(DBSystem.class));

    protected static DBSystem system(){
        return SYSTEM.get();
    }

    protected R result;
    public <S extends SystemCompileCtx<R>> S interpretWith(SystemCompileStream stream) {
        result = doInterpret(stream);
        return (S) this;
    };

    public R result() {
        return result;
    }

    abstract R doInterpret(SystemCompileStream stream);

    public abstract SystemKeyword name();
}
