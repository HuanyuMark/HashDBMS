package org.hashdb.ms.compiler.keyword.ctx.consumer.set;

import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.exception.CommandExecuteException;

import java.util.Set;

/**
 * Date: 2023/11/26 2:03
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
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
