package org.hashdb.ms.compiler.keyword.ctx.consumer;

import org.hashdb.ms.compiler.ConsumerCompileStream;
import org.hashdb.ms.compiler.keyword.KeywordModifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Date: 2023/11/29 14:59
 *
 * @author huanyuMake-pecdle
 */
public class PrecompileResult<T> {
    /**
     * 类型: {@link String} | {@link org.hashdb.ms.compiler.option.OptionCtx} | {@link KeywordModifier} |
     * {@link org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx}
     * 顺序与预编译时遇到的顺序一致
     */
    protected final List<T> values = new LinkedList<>();

    protected ConsumerCtx<?> pipeConsumer;

    protected final ConsumerCompileStream precompileStream;

    public PrecompileResult(ConsumerCompileStream precompileStream) {
        this.precompileStream = precompileStream;
    }

    public List<T> getValues() {
        return values;
    }

    public ConsumerCtx<?> getPipeConsumer() {
        return pipeConsumer;
    }

    public ConsumerCompileStream getPrecompileStream() {
        return precompileStream;
    }
}
