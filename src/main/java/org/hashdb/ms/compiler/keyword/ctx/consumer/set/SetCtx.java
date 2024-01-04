package org.hashdb.ms.compiler.keyword.ctx.consumer.set;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.task.ImmutableChecker;
import org.hashdb.ms.exception.CommandExecuteException;
import org.hashdb.ms.exception.IllegalJavaClassStoredException;
import org.hashdb.ms.exception.StopComplieException;

import java.util.Set;
import java.util.function.Function;

/**
 * Date: 2023/11/29 20:41
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public abstract class SetCtx extends ConsumerCtx<Set<Object>> {
    protected SetCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    protected boolean checkConsumeType(Object consumeType) throws CommandExecuteException {
        Object o = selectOne(consumeType);
        if (!(o instanceof HValue<?> hValue)) {
            return Set.class.isAssignableFrom(o.getClass());
        }
        try {
            return DataType.LIST == DataType.typeofHValue(hValue);
        } catch (IllegalJavaClassStoredException ignore) {
            return false;
        }
    }

    @Override
    protected Function<Set<Object>, ?> compile() throws StopComplieException {
        beforeCompile();
        return executor();
    }

    protected void beforeCompile() {
    }

    @Override
    protected DataType consumableHValueType() {
        return DataType.SET;
    }

    @Override
    protected Class<?> consumableModifiableClass() {
        return DataType.SET.reflect().clazz();
    }

    @Override
    protected Class<?> consumableUnmodifiableClass() {
        return ImmutableChecker.unmodifiableSet;
    }
}
