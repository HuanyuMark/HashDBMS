package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.option.ExpireOpCtx;
import org.hashdb.ms.compiler.option.LongOpCtx;
import org.hashdb.ms.compiler.option.Options;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTaskPriority;
import org.hashdb.ms.data.task.ImmutableChecker;
import org.hashdb.ms.exception.CommandCompileException;
import org.hashdb.ms.exception.CommandExecuteException;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Date: 2023/11/28 19:53
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class WriteSupplierCtx extends SupplierCtx {
    protected final List<Pair> pairs = new LinkedList<>();

    @Override
    public Class<?> supplyType() {
        return ImmutableChecker.unmodifiableList;
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
            SupplierCtx keySupplier = compileInlineCommand();
            if (keySupplier != null) {
                pair.keyOrSupplier = keySupplier;
            } else {
                pair.keyOrSupplier = stream.token();
                stream.next();
            }

            pair.valueCtx = new ValueCtx();

            // 探测一下, 有没有value
            try {
                stream.token();
            } catch (ArrayIndexOutOfBoundsException e) {
                stream.prev();
                String errorToken = stream.token();
                stream.next();
                throw new CommandCompileException("keyword '" + name() + "' require key-value pair to write." + stream.errToken(errorToken));
            }

            // 有可能是内联命令
            SupplierCtx valueSupplier = compileInlineCommand();
            if (valueSupplier != null) {
//                if (!DataType.canStore(valueSupplier.supplyType())) {
//                    throw new CommandExecuteException("can not store the return type of inline command: '" +
//                            valueSupplier.command()
//                            + "'." + stream.errToken(valueSupplier.command()));
//                }
                pair.valueCtx.rawOrSupplier = valueSupplier;
            } else {
                compileJsonValues((dataType, value) -> {
                    pair.valueCtx.rawOrSupplier = value;
                    return false;
                });
            }

            compileOptions(optionCtx -> {
                if (Options.EXPIRE == optionCtx.key()) {
                    pair.valueCtx.expireTime = ((LongOpCtx) optionCtx).value();
                } else if (Options.HEXPIRE == optionCtx.key()) {
                    pair.valueCtx.expireTime = ((LongOpCtx) optionCtx).value();
                    pair.valueCtx.priority = OpsTaskPriority.HIGH;
                } else if (Options.LExpire == optionCtx.key()) {
                    pair.valueCtx.expireTime = ((LongOpCtx) optionCtx).value();
                    pair.valueCtx.priority = OpsTaskPriority.LOW;
                }
                return true;
            });

            pairs.add(pair);
        }
    }

    @Override
    public Supplier<?> compile() {
        doCompile();
        return () -> pairs.stream().map(pair -> {
            String key;
            Object value;
            if (pair.keyOrSupplier instanceof SupplierCtx keySupplier) {
                key = normalizeToQueryKey(getSuppliedValue(keySupplier));
            } else {
                key = (String) pair.keyOrSupplier;
            }
            if (pair.valueCtx.rawOrSupplier instanceof SupplierCtx valueSupplier) {
                value = normalizeToOneValue(getSuppliedValue(valueSupplier));
            } else {
                value = pair.valueCtx.rawOrSupplier;
            }
            HValue<?> oldValue = doMutation(key, value, pair.valueCtx.expireTime, pair.valueCtx.priority);
            return HValue.unwrapData(oldValue);
        }).toList();
    }

    @Nullable
    abstract protected HValue<?> doMutation(String key, Object value, Long expireMillis, OpsTaskPriority priority);

    protected static final class Pair {
        Object keyOrSupplier;
        ValueCtx valueCtx;
    }

    protected static class ValueCtx {
        Object rawOrSupplier;
        Long expireTime;
        OpsTaskPriority priority;
    }
}
