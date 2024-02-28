package org.hashdb.ms.compiler.keyword.ctx.consumer.set;

import org.hashdb.ms.compiler.exception.CommandExecuteException;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;

import java.util.Set;

/**
 * Date: 2023/11/26 2:03
 *
 * @author Huanyu Mark
 */
public abstract class MutableSetCtx extends SetCtx {
    protected MutableSetCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    protected Object operateWithImmutableList(Set<Object> opsTarget) {
        throw new CommandExecuteException("can not modify immutable value from supplier command '" + fatherCompileCtx.command() + "'");
    }
}
