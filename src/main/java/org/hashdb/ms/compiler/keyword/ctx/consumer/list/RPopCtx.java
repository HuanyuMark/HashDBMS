package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Date: 2023/11/29 9:51
 *
 * @author Huanyu Mark
 */
public class RPopCtx extends PopCtx {
    protected RPopCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.RPOP;
    }

    @Override
    List<Object> doPop(List<Object> opsTarget) {
        return IntStream.range(0, popCount).limit(opsTarget.size()).mapToObj(n -> opsTarget.removeLast()).toList();
    }
}
