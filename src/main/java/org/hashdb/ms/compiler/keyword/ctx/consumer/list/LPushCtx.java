package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Date: 2023/11/25 2:42
 *
 * @author Huanyu Mark
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
        opsTarget.addAll(0, other);
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
