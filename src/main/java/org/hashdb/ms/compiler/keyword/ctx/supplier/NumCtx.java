package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.option.ExpireOpCtx;
import org.hashdb.ms.compiler.option.HExpireOpCtx;
import org.hashdb.ms.compiler.option.LExpireOpCtx;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTaskPriority;
import org.hashdb.ms.exception.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Date: 2023/11/29 1:01
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class NumCtx extends SupplierCtx {

    private final List<ArithmeticCtx> arithmeticCtxes = new LinkedList<>();
    @Override
    protected Supplier<?> compile() throws StopComplieException {
        doCompile();
        beforeCompilePipe();
        return () -> arithmeticCtxes.stream()
                .map(arithmeticCtx->{
                    String key;
                    if(arithmeticCtx.keyOrSupplier instanceof SupplierCtx keySupplier) {
                        arithmeticCtx.keyOrSupplier = getSuppliedValue(keySupplier);
                        try {
                            key = normalizeToQueryKey(arithmeticCtx.keyOrSupplier);
                        } catch (UnsupportedQueryKey e) {
                            throw UnsupportedQueryKey.of(name(), keySupplier);
                        }
                    } else {
                        key = ((String) arithmeticCtx.keyOrSupplier);
                    }
                    if(arithmeticCtx.stepOrSupplier instanceof SupplierCtx stepSupplier) {
                        arithmeticCtx.stepOrSupplier = getSuppliedValue(stepSupplier);
                    }
                    return doArithmeticOps(
                            key,
                            arithmeticCtx.stepOrSupplier,
                            arithmeticCtx.millis,
                            arithmeticCtx.priority
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
                token = stream.token();
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }

            ArithmeticCtx arithmeticCtx = new ArithmeticCtx();
            SupplierCtx keySupplier = compileInlineCommand();
            if(keySupplier != null) {
                arithmeticCtx.keyOrSupplier = keySupplier;
                token = stream.token();
            } else {
                arithmeticCtx.keyOrSupplier = token;
                try {
                    token = stream.nextToken();
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new CommandCompileException("keyword '"+name()+"' require key-step pair to operate a number");
                }
            }
            SupplierCtx stepSupplier;
            try {
                stepSupplier = compileInlineCommand();
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new CommandCompileException("keyword '"+name()+"' require key-step pair to operate a number");
            }
            if(stepSupplier!= null) {
                arithmeticCtx.stepOrSupplier = stepSupplier;
            } else {
                if(!token.matches("-?(\\d+)?(\\.)?(\\d+)?") || ".".equals(token) || token.isEmpty()) {
                                        throw new IncreaseUnsupportedException("can not parse step '"+token+"' to number."+stream.errToken(token));
                }
                arithmeticCtx.stepOrSupplier = token;
                stream.next();
            }
            compileOptions(op->{
                if(op instanceof ExpireOpCtx expireCtx) {
                    arithmeticCtx.millis = expireCtx.value();
                } else if(op instanceof HExpireOpCtx expireCtx) {
                    arithmeticCtx.millis = expireCtx.value();
                    arithmeticCtx.priority = OpsTaskPriority.HIGH;
                } else if(op instanceof LExpireOpCtx expireCtx) {
                    arithmeticCtx.millis = expireCtx.value();
                    arithmeticCtx.priority = OpsTaskPriority.LOW;
                }
                addOption(op);
                return true;
            });
            arithmeticCtxes.add(arithmeticCtx);
        }
    }

    @Override
    protected void beforeCompilePipe() {
        if(arithmeticCtxes.isEmpty()) {
            throw new IllegalJavaClassStoredException("keyword '"+name()+"' require at lease one key-step pair to operate a number");
        }
    }

    private @Nullable Object doArithmeticOps(String key, @NotNull Object stepValue, Long millis, OpsTaskPriority priority) {
        Object oneValue = normalizeToOneValueOrElseThrow(stepValue);
        if(oneValue instanceof Double d) {
            if(d.isInfinite()) {
                throw new IncreaseUnsupportedException("step '" + oneValue + "' should be a finite number");
            }
        }
        if(oneValue instanceof Float f) {
            if(f.isInfinite()) {
                throw new IncreaseUnsupportedException("step '" + oneValue + "' should be a finite number");
            }
        }
        if(!(oneValue instanceof String step)) {
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
        HValue<Object> value = (HValue<Object>) stream.db().get(key);
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
                    throw new IncreaseUnsupportedException("can '" + name() + "' string: '" + rawValue + "' with step '" + step + "'");
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
                stream.db().set(key, stepNumber, millis, priority);
                return null;
            }
            default ->
                    throw new IncreaseUnsupportedException("can`t increase type: '" + dataType + "' with step '" + step + "'");
        }
    }

    @Override
    public Class<? extends Number> supplyType() {
        return Number.class;
    }

    abstract Number newValue(Number n1, Number n2);

    protected static class ArithmeticCtx {
        Object keyOrSupplier;
        Object stepOrSupplier;
        Long millis;
        OpsTaskPriority priority;
    }
}
