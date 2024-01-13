package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.exception.CommandExecuteException;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.option.*;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTaskPriority;
import org.hashdb.ms.data.task.UnmodifiableCollections;
import org.hashdb.ms.exception.StopComplieException;
import org.hashdb.ms.exception.UnsupportedQueryKey;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Date: 2023/11/28 21:01
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ExpireCtx extends SupplierCtx {
    @Override
    public void setStream(SupplierCompileStream stream) {
        super.setStream(stream);
        stream().toWrite();
    }

    private final List<KeyCtx> keys = new LinkedList<>();

    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.EXPIRE;
    }

    @Override
    public @NotNull Class<?> supplyType() {
        return UnmodifiableCollections.unmodifiableList;
    }

    @Override
    protected Supplier<?> compile() throws StopComplieException {
        doCompile();
        beforeCompilePipe();
        return executor();
    }

    @Override
    public Supplier<?> executor() {
        return () -> keys.stream().map(keyCtx -> {
            String key;
            if (keyCtx.keyOrSupplier instanceof SupplierCtx supplierCtx) {
                try {
                    key = SupplierCtx.normalizeToQueryKey(exeSupplierCtx(supplierCtx));
                } catch (UnsupportedQueryKey e) {
                    throw UnsupportedQueryKey.of(name(), supplierCtx);
                }
            } else {
                key = ((String) keyCtx.keyOrSupplier);
            }
            HValue<?> value = stream().db().get(key);
            if (value == null) {
                if (keyCtx.keyOrSupplier instanceof SupplierCtx supplierCtx) {
                    throw new CommandExecuteException("key '" + key + "' return form inline command '" + supplierCtx.command() + "' not found");
                }
                throw new CommandExecuteException("key '" + key + "' not found");
            }
            long oldExpireTime = HValue.unwrapExpire(value);
            if (keyCtx.strategyOpCtx == null) {
                ExpireStrategy.DEFAULT.exec(stream().db(), value, keyCtx.millis, keyCtx.deletePriority);
            } else {
                keyCtx.strategyOpCtx.value().exec(stream().db(), value, keyCtx.millis, keyCtx.deletePriority);
            }
            return oldExpireTime;
        }).toList();
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
            KeyCtx keyCtx = new KeyCtx();

            CompileCtx<?> inlineSupplierCtx = compileInlineCommand();
            if (inlineSupplierCtx != null) {
                keyCtx.keyOrSupplier = inlineSupplierCtx;
                try {
                    stream().token();
                } catch (ArrayIndexOutOfBoundsException e) {
                    return;
                }
            } else {
                keyCtx.keyOrSupplier = token;
                stream().next();
            }

            compileOptions(op -> {
                if (op instanceof ExpireStrategyOpCtx expireStrategyOpCtx) {
                    keyCtx.strategyOpCtx = expireStrategyOpCtx;
                } else if (op instanceof ExpireOpCtx expireOpCtx) {
                    keyCtx.millis = expireOpCtx.value();
                } else if (op instanceof LExpireOpCtx lExpireOpCtx) {
                    keyCtx.millis = lExpireOpCtx.value();
                    keyCtx.deletePriority = OpsTaskPriority.LOW;
                } else if (op instanceof HExpireOpCtx hExpireOpCtx) {
                    keyCtx.millis = hExpireOpCtx.value();
                    keyCtx.deletePriority = OpsTaskPriority.HIGH;
                }
                addOption(op);
                return true;
            });
            if (keyCtx.millis == null) {
                throw new CommandCompileException("keyword '" + name() + "' require option '" + Options.EXPIRE + "' of '" + Options.HEXPIRE + "' to set " +
                        "expire time");
            }
            keys.add(keyCtx);
            stream().next();
        }
    }

    @Override
    protected void beforeCompilePipe() {
        if (keys.isEmpty()) {
            throw new CommandCompileException("keyword '" + name() + "' require at lease one key to query");
        }
    }

    protected static class KeyCtx {
        Object keyOrSupplier;
        Long millis;
        OpsTaskPriority deletePriority;
        ExpireStrategyOpCtx strategyOpCtx;
    }
}
