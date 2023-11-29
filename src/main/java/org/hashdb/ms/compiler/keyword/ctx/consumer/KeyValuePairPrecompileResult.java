package org.hashdb.ms.compiler.keyword.ctx.consumer;

import org.hashdb.ms.compiler.ConsumerCompileStream;

/**
 * PlainPair<Object,Object>
 * key 是 string 或者是 supplier
 * value 是 可存储对象 或者是 supplier
 */
public class KeyValuePairPrecompileResult extends PrecompileResult<SetCtx.Pair> {
    public KeyValuePairPrecompileResult(ConsumerCompileStream precompileStream) {
        super(precompileStream);
    }
}
