package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.exception.CommandExecuteException;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.exception.IllegalJavaClassStoredException;
import org.hashdb.ms.exception.StopComplieException;
import org.hashdb.ms.util.UnmodifiableCollections;

import java.util.List;
import java.util.function.Function;

/**
 * Date: 2023/11/29 20:41
 *
 * @author Huanyu Mark
 */
@Slf4j
public abstract class ListCtx extends ConsumerCtx<List<Object>> {
    protected ListCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    protected boolean checkConsumeType(Object consumeType) throws CommandExecuteException {
        Object o = selectOneValue(consumeType);
        if (o instanceof HValue<?> hValue) {
            try {
                return DataType.LIST == DataType.typeofHValue(hValue);
            } catch (IllegalJavaClassStoredException ignore) {
                return false;
            }
        }
        return List.class.isAssignableFrom(o.getClass());
    }

    @Override
    protected Function<List<Object>, ?> compile() throws StopComplieException {
        beforeCompile();
        return executor();
    }

    @Override
    protected DataType consumableHValueType() {
        return DataType.LIST;
    }

    @Override
    protected Class<?> consumableModifiableClass() {
        return DataType.LIST.reflect().clazz();
    }

    @Override
    protected Class<?> consumableUnmodifiableClass() {
        return UnmodifiableCollections.unmodifiableList;
    }
}
