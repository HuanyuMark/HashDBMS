package org.hashdb.ms.compiler.keyword.ctx.consumer;

import org.hashdb.ms.compiler.ConsumerCompileStream;
import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.list.LSetCtx;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;

import java.util.List;

/**
 * Date: 2023/11/29 11:42
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class SetCtx extends InterpretCtx {
    @Override
    public void setStream(ConsumerCompileStream stream) {
        super.setStream(stream);
        stream.toWrite();
    }

    protected KeyValuePairPrecompileResult precompileResult;

    protected SetCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx, List.of(new LSetCtx(fatherCompileCtx)));
    }

    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.SET;
    }

    @Override
    @SuppressWarnings("unchecked")
    public KeyValuePairPrecompileResult getPrecompileResult() {
        return precompileResult;
    }

    @Override
    protected void doPrecompile() {
        precompileResult = new KeyValuePairPrecompileResult(stream());
        while (true) {
            String token;
            try {
                if (compilePipe()) {
                    precompileResult.pipeConsumer = consumerCtx;
                    return;
                }
                filterAllKeywords();
                filterAllOptions();
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }
            Pair pair = new Pair();
            SupplierCtx keySupplier = compileInlineCommand();
            if (keySupplier != null) {
                pair.keyOrSupplier = keySupplier;
            } else {
                pair.keyOrSupplier = stream().token();
                stream().next();
                filterAllKeywords();
            }
            compileJsonValues((dataType, value) -> {
                pair.valueOrSupplier = value;
                return false;
            });
            if (pair.valueOrSupplier == null) {
                throw new CommandCompileException("keyword '" + name() + "' require key-value pair to operate");
            }
            precompileResult.values.add(pair);
        }
    }

    public static class Pair {
        public Object keyOrSupplier;
        public Object valueOrSupplier;
    }
}
