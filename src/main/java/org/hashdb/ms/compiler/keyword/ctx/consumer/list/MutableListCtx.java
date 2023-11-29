package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.task.ImmutableChecker;
import org.hashdb.ms.exception.CommandExecuteException;
import org.hashdb.ms.util.JacksonSerializer;

import java.util.List;

/**
 * Date: 2023/11/26 2:03
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class MutableListCtx extends ListCtx {
    protected MutableListCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    protected Object operateWithImmutableList(List<Object> opsTarget) {
        throw new CommandExecuteException("can not modify immutable value from supplier command '"+fatherCompileCtx.command()+"'");
    }
}
