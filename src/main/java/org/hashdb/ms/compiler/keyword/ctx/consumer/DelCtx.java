package org.hashdb.ms.compiler.keyword.ctx.consumer;

import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.list.LDelCtx;

import java.util.List;

/**
 * Date: 2023/11/29 11:43
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class DelCtx extends InterpretCtx {
    {
        stream.toWrite();
    }

    protected DelCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx, List.of(new LDelCtx(fatherCompileCtx)));
    }

    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.DEL;
    }
}
