package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.exception.CommandExecuteException;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;

import java.util.List;

/**
 * Date: 2023/11/26 2:03
 *
 * @author Huanyu Mark
 */
public abstract class MutableListCtx extends ListCtx {
    protected MutableListCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    protected Object operateWithImmutableList(List<Object> opsTarget) {
        throw new CommandExecuteException("can not modify immutable value from supplier command '" + fatherCompileCtx.command() + "'");
    }
}
