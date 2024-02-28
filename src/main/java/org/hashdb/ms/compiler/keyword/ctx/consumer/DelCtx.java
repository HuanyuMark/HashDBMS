package org.hashdb.ms.compiler.keyword.ctx.consumer;

import org.hashdb.ms.compiler.ConsumerCompileStream;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.list.LDelCtx;

import java.util.List;

/**
 * Date: 2023/11/29 11:43
 *
 * @author Huanyu Mark
 */
public class DelCtx extends InterpretCtx {
    @Override
    public void setStream(ConsumerCompileStream stream) {
        super.setStream(stream);
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
