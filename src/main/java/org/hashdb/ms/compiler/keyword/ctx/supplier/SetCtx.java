package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.option.ExpireOpCtx;
import org.hashdb.ms.compiler.option.LongOpCtx;
import org.hashdb.ms.compiler.option.Options;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.data.OpsTaskPriority;
import org.hashdb.ms.data.task.UnmodifiedChecker;
import org.hashdb.ms.exception.CommandExecuteException;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Date: 2023/11/24 16:20
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class SetCtx extends SupplierCtx {

    protected final List<Pair> pairs = new LinkedList<>();

    @Override
    public Class<?> supplyType() {
        return UnmodifiedChecker.unmodifiableList;
    }

    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.SET;
    }

    @Override
    public Supplier<?> compile() {
        doCompile();
        return () -> pairs.stream().map(pair -> {
            String key;
            Object value;
            if (pair.keyOrSupplier instanceof SupplierCtx keySupplier) {
                key = normalizeToQueryKey(keySupplier.compileResult().get());
            } else {
                key = (String) pair.keyOrSupplier;
            }
            if (pair.valueCtx.rawOrSupplier instanceof SupplierCtx valueSupplier) {
                value = valueSupplier.compileResult().get();
            } else {
                value = pair.valueCtx.rawOrSupplier;
            }
            HValue<?> oldValue = stream.db().setImmediately(key, value, pair.valueCtx.expireTime, pair.valueCtx.priority);
            return consumeWithConsumer(HValue.unwrap(oldValue));
        }).toList();
    }

    private void doCompile() {
        while (true) {
            try {
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

            // 有可能是内联命令
            SupplierCtx valueSupplier = compileInlineCommand();
            if (valueSupplier != null) {
                if (!DataType.canStore(valueSupplier.supplyType())) {
                    throw new CommandExecuteException("can not store the return type of inline command: '" +
                            valueSupplier.command()
                            + "'." + stream.errToken(valueSupplier.command()));
                }
                pair.valueCtx.rawOrSupplier = valueSupplier;
            } else {
                compileJsonValues(value -> {
                    pair.valueCtx.rawOrSupplier = value;
                    return false;
                });
            }

            compileOptions(optionCtx -> {
                if (Options.HEXPIRE == optionCtx.key()) {
                    pair.valueCtx.expireTime = ((ExpireOpCtx) optionCtx).value();
                    pair.valueCtx.priority = OpsTaskPriority.HIGH;
                } else if (Options.EXPIRE == optionCtx.key()) {
                    pair.valueCtx.expireTime = ((LongOpCtx) optionCtx).value();
                    pair.valueCtx.priority = OpsTaskPriority.LOW;
                }
                return true;
            });

            pairs.add(pair);
        }
    }

    private static final class Pair {
        Object keyOrSupplier;
        ValueCtx valueCtx;
    }

    private static class ValueCtx {
        Object rawOrSupplier;
        Long expireTime;
        OpsTaskPriority priority;
    }
}
