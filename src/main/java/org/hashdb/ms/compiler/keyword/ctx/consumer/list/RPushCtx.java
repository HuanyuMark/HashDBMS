package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Date: 2023/11/25 2:45
 *
 * @author Huanyu Mark
 */
public class RPushCtx extends PushCtx {

    protected RPushCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.RPUSH;
    }

    @Override
    protected void doPushRaw(@NotNull List<Object> opsTarget, Object rawValue) {
        opsTarget.addLast(rawValue);
    }

    @Override
    protected void doPushCollection(List<Object> opsTarget, Collection<Object> other) {
        opsTarget.addAll(other);
    }
}
