package org.hashdb.ms.compiler.keyword.ctx.consumer;

/**
 * Date: 2023/11/29 14:49
 *
 * @author huanyuMake-pecdle
 */
public interface Precompilable {
    void compileWithPrecompileResult(PrecompileResult<?> result);
}
