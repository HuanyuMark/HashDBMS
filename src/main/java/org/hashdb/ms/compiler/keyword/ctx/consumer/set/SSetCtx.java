package org.hashdb.ms.compiler.keyword.ctx.consumer.set;

import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.Precompilable;
import org.hashdb.ms.compiler.keyword.ctx.consumer.PrecompileResult;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.task.ImmutableChecker;

import java.util.Set;

/**
 * Date: 2023/12/27 11:02
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class SSetCtx extends MutableSetCtx implements Precompilable {
    protected SSetCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    public Class<?> supplyType() {
        return ImmutableChecker.unmodifiableList;
    }

    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.SSET;
    }

    @Override
    protected void beforeCompile() {

    }

    @Override
    protected Object operateWithMutableList(Set<Object> opsTarget) {
        return null;
    }

    @Override
    protected Object operateWithHValue(HValue<Set<Object>> opsTarget) {
        return null;
    }

    @Override
    public void compileWithPrecompileResult(PrecompileResult<?> result) {

    }
}