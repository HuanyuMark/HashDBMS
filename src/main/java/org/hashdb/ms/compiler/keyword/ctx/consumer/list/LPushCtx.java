package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.compiler.option.DestructOpCtx;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.task.ImmutableChecker;
import org.hashdb.ms.exception.CommandExecuteException;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Date: 2023/11/25 2:42
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class LPushCtx extends PushCtx {

    protected LPushCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    protected void doPushRaw(@NotNull List<Object> opsTarget, Object rawValue) {
        opsTarget.addFirst(rawValue);
    }

    @Override
    protected void doPushCollection(List<Object> opsTarget, Collection<Object> other) {
        opsTarget.addAll(0,other);
    }

    @Override
    protected List<Object> beforePush() {
        return values.reversed();
    }

    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.LPUSH;
    }
}
