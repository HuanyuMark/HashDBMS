package org.hashdb.ms.compiler.keyword.ctx.consumer;

import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.exception.CommandExecuteException;
import org.hashdb.ms.compiler.exception.CommandInterpretException;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.exception.StopComplieException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

/**
 * Date: 2023/11/29 16:29
 *
 * @author huanyuMake-pecdle
 */
public abstract class InterpretCtx extends ConsumerCtx<Object> {
    protected PrecompileResult<Object> precompileResult;
    protected final List<ConsumerCtx<?>> consumerCtxCandidates;

    protected InterpretCtx(CompileCtx<?> fatherCompileCtx, List<ConsumerCtx<?>> consumerCtxCandidates) {
        super(fatherCompileCtx);
        this.consumerCtxCandidates = consumerCtxCandidates;
    }

    @SuppressWarnings("unchecked")
    public <V, P extends PrecompileResult<V>> P getPrecompileResult() {
        return (P) precompileResult;
    }

    @Override
    public @NotNull Class<?> supplyType() {
        return Object.class;
    }

    @Override
    protected boolean checkConsumeType(Object consumeType) {
        return true;
    }

    @Override
    protected Function<Object, ?> compile() throws StopComplieException {
        doPrecompile();
        beforeCompilePipe();
        return executor();
    }

    @Override
    protected Function<Object, ?> executor() {
        return opsTarget -> {
            List<ConsumerCtx<?>> matched = consumerCtxCandidates.stream().filter(consumer -> {
                try {
                    return consumer.checkConsumeType(opsTarget);
                } catch (CommandExecuteException ignore) {
                    return false;
                }
            }).toList();
            if (matched.size() != 1) {
                throw new CommandInterpretException("command ambiguous. can not match a unique '" + name() + "' keyword to execute. matched keyword: " + matched);
            }
            @SuppressWarnings("unchecked")
            ConsumerCtx<Object> selected = (ConsumerCtx<Object>) matched.getFirst();
            if (selected instanceof Precompilable precompilable) {
                precompilable.compileWithPrecompileResult(getPrecompileResult());
            } else {
                stream().reset();
                selected.compileWithStream(stream());
            }
            return selected.compile().apply(opsTarget);
        };
    }

    protected void doPrecompile() {
        precompileResult = new PrecompileResult<>(stream());
        while (true) {
            try {
                if (compilePipe()) {
                    getPrecompileResult().pipeConsumer = consumerCtx;
                    return;
                }
                filterAllKeywords();
                compileOptions(o -> {
                    getPrecompileResult().values.add(o);
                    addOption(o);
                    return false;
                });
                SupplierCtx keySupplierCtx = compileInlineCommand();
                if (keySupplierCtx != null) {
                    getPrecompileResult().values.add(keySupplierCtx);
                } else {
                    String token = stream().token();
                    if (!compileParameter(true, (dataType, parameter) -> {
                        getPrecompileResult().values.add(token);
                        return false;
                    })) {
                        Object customized = compileRawString(token);
                        if (customized == null) {
                            getPrecompileResult().values.add(token);
                            stream().next();
                        } else {
                            getPrecompileResult().values.add(customized);
                        }
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }
        }
    }

    /**
     * 自定义解析字符串
     * 如果返回null, 则直接当作普通字符串, 加入预编译结果里, 否则, 将自定义编译的
     * 结果加入预编译结果中. 如果自定义编译结果不为空, 则调用者必须自定义stream的cursor指向!
     *
     * @param unknownToken 原string token
     * @return 自定义解析的对象
     */
    protected Object compileRawString(String unknownToken) {
        return null;
    }

    @Override
    protected void beforeCompilePipe() {
        if (getPrecompileResult().values.isEmpty()) {
            throw new CommandCompileException("keyword '" + name() + "' require at lease one key to query");
        }
    }

    @Override
    protected DataType consumableHValueType() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Class<?> consumableModifiableClass() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Class<?> consumableUnmodifiableClass() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object operateWithMutableList(Object opsTarget) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object operateWithImmutableList(Object opsTarget) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object operateWithHValue(HValue<Object> opsTarget) {
        throw new UnsupportedOperationException();
    }
}
