package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.exception.CommandExecuteException;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.option.LimitOpCtx;
import org.hashdb.ms.compiler.option.Options;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.util.UnmodifiableCollections;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Date: 2023/11/29 9:54
 *
 * @author Huanyu Mark
 */
public class LReverseCtx extends ListCtx {
    protected boolean self = false;

    protected Long limit;

    protected Object keyOrSupplier;

    protected LReverseCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    public @NotNull Class<?> supplyType() {
        return self ? List.class : UnmodifiableCollections.unmodifiableList;
    }

    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.REVERSE;
    }

    @Override
    protected Object operateWithMutableList(List<Object> opsTarget) {
        if (self) {
            Collections.reverse(opsTarget);
            return opsTarget;
        }
        return Collections.unmodifiableList(opsTarget.reversed());
    }

    @Override
    protected Object operateWithImmutableList(List<Object> opsTarget) {
        if (self) {
            throw new CommandExecuteException("can not reverse a immutable list");
        }
        return Collections.unmodifiableList(opsTarget.reversed());
    }

    @Override
    protected Object operateWithHValue(HValue<List<Object>> opsValue) {
        if (self) {
            Collections.reverse(opsValue.data());
            return opsValue.data();
        }
        return Collections.unmodifiableList(opsValue.data());
    }

    private void doCompile() {
        while (true) {
            try {
                if (compilePipe()) {
                    return;
                }
                filterAllKeywords();
                if (!compileOptions(op -> {
                    if (op instanceof LimitOpCtx limitOpCtx) {
                        limit = limitOpCtx.value();
                    }
                    addOption(op);
                    return true;
                })) {
                    throw new CommandCompileException("keyword '" + name() + "' permit to set option '" + Options.LIMIT + "'");
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }
        }
    }

    @Override
    protected void beforeCompilePipe() {
        super.beforeCompilePipe();
    }
}
