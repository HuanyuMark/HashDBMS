package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Date: 2023/11/29 9:58
 *
 * @author huanyuMake-pecdle
 */
public class LPopRPush extends PopPushCtx {
    protected LPopRPush(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.LPOPRPUSH;
    }

    @Override
    protected List<Object> doPop(List<Object> opsTarget) {
        int count = popCount;
        LinkedList<Object> result = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            result.add(opsTarget.removeFirst());
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    protected void doPush(List<Object> opsTarget, Stream<Object> values) {
        values.sequential().forEach(opsTarget::add);
    }
}
