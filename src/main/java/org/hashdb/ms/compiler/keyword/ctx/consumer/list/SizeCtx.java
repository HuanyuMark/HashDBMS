package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.SingletonCompileCtx;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.exception.StopComplieException;

import java.util.List;
import java.util.function.Function;

/**
 * Date: 2023/11/29 9:54
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class SizeCtx extends MutableListCtx implements SingletonCompileCtx {
    protected SizeCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }
    @Override
    public Class<?> supplyType() {
        return Integer.class;
    }

    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.SIZE;
    }

    @Override
    protected Object operateWithMutableList(List<Object> opsTarget) {
        return operateWithImmutableList(opsTarget);
    }

    @Override
    protected Object operateWithHValue(HValue<List<Object>> opsTarget) {
        return operateWithMutableList(opsTarget.data());
    }

    @Override
    protected Object operateWithImmutableList(List<Object> opsTarget) {
        return opsTarget.size();
    }
}
