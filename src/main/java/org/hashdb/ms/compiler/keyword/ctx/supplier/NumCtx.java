package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.option.ExpireOpCtx;
import org.hashdb.ms.compiler.option.HExpireOpCtx;
import org.hashdb.ms.compiler.option.LExpireOpCtx;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTaskPriority;
import org.hashdb.ms.exception.IllegalJavaClassStoredException;
import org.hashdb.ms.exception.IncreaseUnsupportedException;
import org.hashdb.ms.exception.StopComplieException;
import org.hashdb.ms.exception.UnsupportedQueryKey;
import org.hashdb.ms.util.UnmodifiableCollections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Date: 2023/11/29 1:01
 *
 * @author huanyuMake-pecdle
 */
public abstract class NumCtx extends SupplierCtx {

    private final List<ArithmeticPair> arithmeticPairs = new LinkedList<>();

    @Override
    protected Supplier<?> compile() throws StopComplieException {
        doCompile();
        beforeCompilePipe();
        return executor();
    }

    @Override
    public Supplier<?> executor() {
        return () -> arithmeticPairs.stream()
                .map(arithmeticPair -> {
                    String key;
                    if (arithmeticPair.keyOrSupplier instanceof SupplierCtx keySupplier) {
                        try {
                            key = normalizeToQueryKey(exeSupplierCtx(keySupplier));
                        } catch (UnsupportedQueryKey e) {
                            throw UnsupportedQueryKey.of(name(), keySupplier);
                        }
                    } else {
                        key = ((String) arithmeticPair.keyOrSupplier);
                    }
                    return doArithmeticOps(
                            key,
                            arithmeticPair.stepOrSupplier instanceof SupplierCtx stepSupplier ?
                                    exeSupplierCtx(stepSupplier) : arithmeticPair.stepOrSupplier,
                            arithmeticPair.millis,
                            arithmeticPair.priority
                    );
                })
                .toList();
    }

    private void doCompile() {
        while (true) {
            String token;
            try {
                if (compilePipe()) {
                    return;
                }
                filterAllKeywords();
                filterAllOptions();
                token = stream().token();
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }
            // inc key step
            ArithmeticPair arithmeticPair = new ArithmeticPair();
            // inc (inlineCommand)
            SupplierCtx keySupplier = compileInlineCommand();
            if (keySupplier != null) {
                arithmeticPair.keyOrSupplier = keySupplier;
                token = stream().token();
            } else {
                // inc $param
                if (!compileParameter(false, (dataType, parameter) -> {
                    if (dataType != null) {
                        throw new CommandCompileException("the key to query should be a raw string of an legal inline command");
                    }
                    arithmeticPair.keyOrSupplier = parameter;
                    return false;
                })) {
                    // inc rawStringKey
                    arithmeticPair.keyOrSupplier = token;
                    try {
                        token = stream().nextToken();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new CommandCompileException("keyword '" + name() + "' require key-step pair to operate a number");
                    }
                }
            }
            // inc key step
            SupplierCtx stepSupplier;
            try {
                stepSupplier = compileInlineCommand();
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new CommandCompileException("keyword '" + name() + "' require key-step pair to operate a number");
            }
            // inc key (inlineCommand)
            if (stepSupplier != null) {
                arithmeticPair.stepOrSupplier = stepSupplier;
            } else {
                // inc key $param
                boolean isParameter = compileParameter(false, (dataType, parameter) -> {
                    if (dataType != null && dataType != DataType.STRING && dataType != DataType.NUMBER) {
                        throw new CommandCompileException("can not parse parameter '' to number." + stream().errToken(parameter.getParameterName()));
                    }
                    arithmeticPair.stepOrSupplier = parameter;
                    return false;
                });

                // 这里为什么不预先转为Number对象呢？
                // inc key rowStepString
                if (!isParameter) {
                    if (!token.matches("-?(\\d+)?(\\.)?(\\d+)?") || ".".equals(token) || token.isEmpty()) {
                        throw new IncreaseUnsupportedException("can not parse step '" + token + "' to number." + stream().errToken(token));
                    }
                    arithmeticPair.stepOrSupplier = token;
                }
                stream().next();
            }
            // inc key step [-options]
            compileOptions(op -> {
                if (op instanceof ExpireOpCtx expireCtx) {
                    // inc key step -ep=millis
                    arithmeticPair.millis = expireCtx.value();
                } else if (op instanceof HExpireOpCtx expireCtx) {
                    // inc key step -hep=millis
                    arithmeticPair.millis = expireCtx.value();
                    arithmeticPair.priority = OpsTaskPriority.HIGH;
                } else if (op instanceof LExpireOpCtx expireCtx) {
                    // inc key step -lep=millis
                    arithmeticPair.millis = expireCtx.value();
                    arithmeticPair.priority = OpsTaskPriority.LOW;
                }
                addOption(op);
                return true;
            });
            arithmeticPairs.add(arithmeticPair);
        }
    }

    @Override
    protected void beforeCompilePipe() {
        if (arithmeticPairs.isEmpty()) {
            throw new IllegalJavaClassStoredException("keyword '" + name() + "' require at lease one key-step pair to operate a number");
        }
    }

    private @Nullable Object doArithmeticOps(String key, @NotNull Object stepValue, Long millis, OpsTaskPriority priority) {
        Object oneValue = selectOneKeyOrElseThrow(stepValue);
        if (oneValue instanceof Double d) {
            if (d.isInfinite()) {
                throw new IncreaseUnsupportedException("step '" + oneValue + "' should be a finite number");
            }
        }
        if (oneValue instanceof Float f) {
            if (f.isInfinite()) {
                throw new IncreaseUnsupportedException("step '" + oneValue + "' should be a finite number");
            }
        }
        if (!(oneValue instanceof String step)) {
            throw new IncreaseUnsupportedException("step '" + oneValue + "' should be a number or number like string");
        }
        Number stepNumber;
        try {
            if (step.contains(".")) {
                stepNumber = Double.valueOf(step);
            } else {
                stepNumber = Long.valueOf(step);
            }
        } catch (NumberFormatException e) {
            throw new IncreaseUnsupportedException("step '" + step + "' must be a number");
        }
        @SuppressWarnings("unchecked")
        HValue<Object> value = (HValue<Object>) stream().db().get(key);
        final DataType dataType = DataType.typeofHValue(value);
        switch (dataType) {
            case STRING -> {
                final Object oldValue = value.data();
                final String rawValue = ((String) value.data());
                final Number newValue;
                try {
                    if (rawValue.contains(".")) {
                        value.data(newValue(Double.parseDouble(rawValue), stepNumber));
                    } else {
                        value.data(newValue(Long.parseLong(rawValue), stepNumber));
                    }
                } catch (NumberFormatException e) {
                    throw new IncreaseUnsupportedException("can`t '" + name() + "' string: '" + rawValue + "' with step '" + step + "'");
                } catch (ClassCastException e) {
                    throw IllegalJavaClassStoredException.of(rawValue.getClass());
                }
                return oldValue;
            }
            case NUMBER -> {
                final Object oldValue = value.data();
                final Number rawValue = (Number) value.data();
                value.data(newValue(rawValue, stepNumber));
                return oldValue;
            }
            case NULL -> {
                stream().db().set(key, stepNumber, millis, priority);
                return null;
            }
            default ->
                    throw new IncreaseUnsupportedException("can`t increase type: '" + dataType + "' with step '" + step + "'");
        }
    }

    @Override
    public @NotNull Class<?> supplyType() {
        return UnmodifiableCollections.unmodifiableList;
    }

    abstract Number newValue(Number n1, Number n2);

    protected static class ArithmeticPair {
        Object keyOrSupplier;
        Object stepOrSupplier;
        Long millis;
        OpsTaskPriority priority;
    }
}
