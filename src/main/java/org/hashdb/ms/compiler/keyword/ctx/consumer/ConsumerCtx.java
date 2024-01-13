package org.hashdb.ms.compiler.keyword.ctx.consumer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.ConsumerCompileStream;
import org.hashdb.ms.compiler.exception.CommandExecuteException;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.task.UnmodifiableCollections;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.exception.IllegalJavaClassStoredException;
import org.hashdb.ms.exception.StopComplieException;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Date: 2023/11/25 13:28
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public abstract class ConsumerCtx<I> extends CompileCtx<ConsumerCompileStream> {

    @JsonIgnore
    protected final CompileCtx<?> fatherCompileCtx;
    @JsonIgnore
    protected Function<I, ?> compileResult;

    protected ConsumerCtx(CompileCtx<?> fatherCompileCtx) {
        this.fatherCompileCtx = fatherCompileCtx;
    }

    @Override
    abstract public ConsumerKeyword name();

    abstract protected boolean checkConsumeType(Object consumeType) throws CommandExecuteException;

    public Function<I, ?> compileWithStream(ConsumerCompileStream compileStream) throws StopComplieException {
        if (compileResult != null) {
            throw new DBSystemException(getClass().getSimpleName() + " is finish compilation");
        }
        setStream(compileStream);
        // 必须要先在当前线程中编译, 提前发现编译错误
        Function<I, ?> consumerTask = compile();
        compileResult = opsTarget -> callConsumer(consumerTask.apply(opsTarget));
        return compileResult;
    }

    /**
     * 编译, 并生成使用对应Ctx的执行器
     */
    abstract protected Function<I, ?> compile() throws StopComplieException;

    protected Function<I, ?> executor() {
        return opsTarget -> {
            Object oneValue = selectOneValue(opsTarget);
            if (oneValue instanceof HValue<?> hValue) {
                try {
                    if (consumableHValueType() == DataType.typeofHValue(hValue)) {
                        return operateWithHValue((HValue<I>) hValue);
                    }
                } catch (IllegalJavaClassStoredException ignore) {
                    try {
                        throw new CommandExecuteException("keyword '" + name() + "' can not consume return type from '" + stream().fatherCommand() + "'." + stream().errToken(stream().token()));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new CommandExecuteException("keyword '" + name() + "' can not consume return type from '" + stream().fatherCommand() + "'." + stream().errToken(""));
                    }
                }
            }

            if (consumableModifiableClass().isAssignableFrom(oneValue.getClass())) {
                return operateWithMutableList((I) oneValue);
            }

            if (consumableUnmodifiableClass().isAssignableFrom(oneValue.getClass())) {
                return operateWithImmutableList((I) oneValue);
            }

            throw new CommandExecuteException("keyword '" + name() + "' can not consume return type from '" + stream().fatherCommand() + "'." + stream().errToken(""));
        };
    }

    public Object consume(I opsTarget) {
        if (opsTarget == null) {
            throw new CommandExecuteException("keyword '" + name() + "' can not consume type: 'null' return from '" + stream().fatherCommand() + "'." + stream().errToken(stream().token()));
        }
        if (!checkConsumeType(opsTarget)) {
            throw new CommandExecuteException("keyword '" + name() + "' can not consume type: '" + DataType.typeOfRawValue(opsTarget) + "' return from '" + stream().fatherCommand() + "'." + stream().errToken(stream().token()));
        }
        return compileResult.apply(opsTarget);
    }

    protected HValue<List<?>> extractHValue(@NotNull List<?> returnList) throws CommandExecuteException {
        if (!(returnList.getFirst() instanceof HValue<?> opsValue)) {
            throw new CommandExecuteException("keyword '" + name() + "' con not consume return value of supplier command '" + fatherCompileCtx.command() + "'");
        }
        DataType returnType = DataType.typeofHValue(opsValue);
        if (returnType != DataType.LIST) {
            throw new CommandExecuteException("keyword '" + name() + "' con not consume return type '" + returnType + "' of supplier command '" + fatherCompileCtx.command() + "'");
        }
        return (HValue<List<?>>) opsValue;
    }

    protected Object selectOneValue(Object opsTarget) throws CommandExecuteException {
        Function<Collection<?>, Object> selectOne = collection -> {
            if (collection.isEmpty()) {
                try {
                    throw new CommandExecuteException("keyword '" + name() + "' can not consume return value '[]' from '" + stream().fatherCommand() + "'." + stream().errToken(stream().token()));
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new CommandExecuteException("keyword '" + name() + "' can not consume return value '[]' from '" + stream().fatherCommand() + "'." + stream().errToken(""));
                }
            }
            if (collection.size() == 1) {
                Object one = collection.stream().limit(1).findFirst().orElseThrow();
                return selectOneValue(one);
            }
            try {
                throw new CommandExecuteException("can not select a unique operation target from '" + stream().fatherCommand() + "'." + stream().errToken(stream().token()));
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new CommandExecuteException("can not select a unique operation target from '" + stream().fatherCommand() + "'." + stream().errToken(""));
            }
        };
        if (UnmodifiableCollections.isUnmodifiableList(opsTarget.getClass()) ||
                UnmodifiableCollections.isUnmodifiableSet(opsTarget.getClass())) {
            return selectOne.apply((Collection<?>) opsTarget);
        }
        return opsTarget;
    }

    protected abstract DataType consumableHValueType();

    protected abstract Class<?> consumableModifiableClass();

    protected abstract Class<?> consumableUnmodifiableClass();

    abstract protected Object operateWithMutableList(I opsTarget);

    abstract protected Object operateWithImmutableList(I opsTarget);

    abstract protected Object operateWithHValue(HValue<I> opsTarget);

    protected void beforeCompile() {
    }
}
