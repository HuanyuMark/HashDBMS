package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.option.LongOpCtx;
import org.hashdb.ms.compiler.option.Options;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTaskPriority;
import org.hashdb.ms.data.task.UnmodifiableCollections;
import org.hashdb.ms.exception.UnsupportedQueryKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Date: 2023/11/28 19:53
 *
 * @author huanyuMake-pecdle
 */
public abstract class WriteSupplierCtx extends SupplierCtx {
    @Override
    public void setStream(SupplierCompileStream stream) {
        super.setStream(stream);
        stream().toWrite();
    }

    protected final List<Pair> pairs = new LinkedList<>();

    @Override
    public @NotNull Class<?> supplyType() {
        return UnmodifiableCollections.unmodifiableList;
    }

    @Override
    protected void beforeCompilePipe() {
        if (pairs.isEmpty()) {
            throw new CommandCompileException("keyword '" + name() + "' require at lease one key-value pair to store");
        }
    }

    protected void doCompile() {
        while (true) {
            try {
                if (compilePipe()) {
                    return;
                }
                filterAllKeywords();
                filterAllOptions();
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }
            Pair pair = new Pair();
            // 有可能是内联命令
            SupplierCtx keySupplier;
            try {
                keySupplier = compileInlineCommand();
            } catch (ArrayIndexOutOfBoundsException e) {
                stream().prev();
                String errorToken = stream().token();
                throw new CommandCompileException("keyword '" + name() + "' require key-value pair to write." + stream().errToken(errorToken));
            }
            if (keySupplier != null) {
                pair.keyOrSupplier = keySupplier;
            } else {
                boolean isParameter = compileParameter(false, (dataType, value) -> {
                    pair.keyOrSupplier = value;
                    return false;
                });
                if (!isParameter) {
                    String token = stream().token();
                    if (isOriginalString(token) && token.charAt(2) != '$') {
                        throw new CommandCompileException("parameter name should be started with '$'." + stream().errToken(token));
                    }
                    pair.keyOrSupplier = token;
                    stream().next();
                }
            }

            // 有可能是内联命令
            SupplierCtx valueSupplier;
            try {
                valueSupplier = compileInlineCommand();
            } catch (ArrayIndexOutOfBoundsException e) {
                // 如果没有value
                stream().prev();
                String errorToken = stream().token();
                throw new CommandCompileException("keyword '" + name() + "' require key-value pair to write." + stream().errToken(errorToken));
            }

            if (valueSupplier != null) {
                pair.rawOrSupplierValue = valueSupplier;
            } else {
                try {
                    boolean isParameter = compileParameter(true, (dataType, value) -> {
                        if (value instanceof SupplierCtx inlineCmd && inlineCmd.storeType().unsupportedClone(inlineCmd.supplyType())) {
                            throw new CommandCompileException("can not writer value type '" + inlineCmd.storeType() + "' of '" + inlineCmd.command() + "'");
                        }
                        pair.rawOrSupplierValue = value;
                        return false;
                    });
                    if (!isParameter) {
                        compileJsonValues((dataType, value) -> {
                            if (dataType != null) {
                                pair.rawOrSupplierValue = dataType.clone(value);
                                pair.storeType = dataType;
                                return false;
                            }
                            pair.rawOrSupplierValue = value;
                            return false;
                        });
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    return;
                }
            }

            compileOptions(optionCtx -> {
                if (Options.EXPIRE == optionCtx.key()) {
                    pair.expireTime = ((LongOpCtx) optionCtx).value();
                } else if (Options.HEXPIRE == optionCtx.key()) {
                    pair.expireTime = ((LongOpCtx) optionCtx).value();
                    pair.priority = OpsTaskPriority.HIGH;
                } else if (Options.LEXPIRE == optionCtx.key()) {
                    pair.expireTime = ((LongOpCtx) optionCtx).value();
                    pair.priority = OpsTaskPriority.LOW;
                }
                addOption(optionCtx);
                return true;
            });

            pairs.add(pair);
        }
    }

    @Override
    public Supplier<?> compile() {
        doCompile();
        return executor();
    }


    @Override
    public Supplier<?> executor() {
        return () -> pairs.stream().map(pair -> {
            String rawKey;
            Object rawValue;
            if (pair.keyOrSupplier instanceof SupplierCtx keySupplier) {
                try {
                    rawKey = normalizeToQueryKey(exeSupplierCtx(keySupplier));
                } catch (UnsupportedQueryKey e) {
                    throw UnsupportedQueryKey.of(name(), keySupplier);
                }
            } else {
                rawKey = (String) pair.keyOrSupplier;
            }
            if (pair.rawOrSupplierValue instanceof SupplierCtx valueSupplier) {
                rawValue = exeSupplierCtx(valueSupplier, true);
            } else {
                rawValue = pair.rawOrSupplierValue;
            }
            HValue<?> oldValue = doMutation(rawKey, rawValue, pair.expireTime, pair.priority);
            return HValue.unwrapData(oldValue);
        }).toList();
    }

    @Nullable
    abstract protected HValue<?> doMutation(String key, Object rawValue, Long expireMillis, OpsTaskPriority priority);

    protected static final class Pair {
        Object keyOrSupplier;
        Object rawOrSupplierValue;
        Long expireTime;
        OpsTaskPriority priority;
        DataType storeType;
    }
}
