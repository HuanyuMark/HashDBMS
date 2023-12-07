package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.task.ImmutableChecker;
import org.hashdb.ms.exception.CommandExecuteException;
import org.hashdb.ms.exception.IllegalJavaClassStoredException;
import org.hashdb.ms.exception.StopComplieException;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Date: 2023/11/29 20:41
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public abstract class ListCtx extends ConsumerCtx<List<Object>> {
    protected ListCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    protected boolean checkConsumeType(Object consumeType) throws CommandExecuteException {
        Object o = selectOne(consumeType);
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
    protected Function<List<Object>, ?> executor() {
        return opsTarget -> {
            Object oneValue = selectOne(opsTarget);
            if (oneValue instanceof HValue<?> hValue) {
                try {
                    if (DataType.LIST == DataType.typeofHValue(hValue)) {
                        return operateWithHValue((HValue<List<Object>>) hValue);
                    }
                } catch (IllegalJavaClassStoredException ignore) {
                }
            }

            if (oneValue instanceof List<?> list) {
                if (ImmutableChecker.unmodifiableList.isAssignableFrom(list.getClass())) {
                    return operateWithImmutableList((List<Object>) list);
                }
                return operateWithMutableList((List<Object>) list);
            }
            throw new CommandExecuteException("keyword '" + name() + "' can not consume return type from '" + stream.fatherCommand() + "'." + stream.errToken(""));
        };
    }

    protected Object selectOne(Object opsTarget) throws CommandExecuteException {
        Function<Collection<?>, Object> selectOne = collection -> {
            if (collection.isEmpty()) {
                throw new CommandExecuteException("keyword '" + name() + "' can not consume return value '[]' from '" + stream.fatherCommand() + "'." + stream.errToken(""));
            }
            if (collection.size() == 1) {
                Object one = collection.stream().limit(1).findFirst().orElseThrow();
                return selectOne(one);
            }
            throw new CommandExecuteException("can not select a unique operation target from '" + stream.fatherCommand() + "'." + stream.errToken(""));
        };
        if (ImmutableChecker.isUnmodifiableList(opsTarget.getClass()) ||
                ImmutableChecker.isUnmodifiableSet(opsTarget.getClass())) {
            return selectOne.apply((Collection<?>) opsTarget);
        }
        return opsTarget;
    }

    void beforeCompile() {
    }

    ;

    abstract protected Object operateWithMutableList(List<Object> opsTarget);

    abstract protected Object operateWithImmutableList(List<Object> opsTarget);

    abstract protected Object operateWithHValue(HValue<List<Object>> opsTarget);
}
