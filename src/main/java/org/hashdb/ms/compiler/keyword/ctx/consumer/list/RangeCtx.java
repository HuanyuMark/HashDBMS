package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.exception.CommandExecuteException;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.task.UnmodifiableCollections;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Date: 2023/11/29 19:02
 *
 * @author huanyuMake-pecdle
 */
public class RangeCtx extends ListCtx {

    protected Object startIndexOrSupplier;
    protected Object endIndexOrSupplier;

    protected RangeCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    public @NotNull Class<?> supplyType() {
        return UnmodifiableCollections.unmodifiableList;
    }

    @Override
    public ConsumerKeyword name() {
        return null;
    }

    @Override
    protected void beforeCompile() {
        doCompile();
        beforeCompilePipe();
    }

    @Override
    protected Object operateWithMutableList(List<Object> opsTarget) {
        return operateWithImmutableList(opsTarget);
    }

    @Override
    protected Object operateWithImmutableList(List<Object> opsTarget) {
        long startIndex;
        if (startIndexOrSupplier instanceof SupplierCtx startIndexSupplier) {
            Object o = exeSupplierCtx(startIndexSupplier);
            if (o instanceof Number n) {
                startIndex = n.longValue();
            } else {
                throw new CommandExecuteException("keyword '" + name() + "' can not consume return form inline command '" + startIndexSupplier.command() + "'");
            }
        } else {
            startIndex = (Long) startIndexOrSupplier;
        }
        startIndex = startIndex > 0 ? startIndex : opsTarget.size() + startIndex;
        if (endIndexOrSupplier == null) {
            return opsTarget.stream().skip(startIndex).toList();
        }
        long endIndex;
        if (endIndexOrSupplier instanceof SupplierCtx endIndexSupplier) {
            Object o = exeSupplierCtx(endIndexSupplier);
            if (o instanceof Number n) {
                endIndex = n.longValue();
            } else {
                throw new CommandExecuteException("keyword '" + name() + "' can not consume return value form inline command '" + endIndexSupplier.command() + "'");
            }
        } else {
            endIndex = (Long) endIndexOrSupplier;
        }
        if (endIndex < startIndex) {
            throw new CommandExecuteException("endIndex " + (
                    endIndexOrSupplier instanceof SupplierCtx endIndexSupplier ?
                            "from inline command '" + endIndexSupplier.command() + "'" :
                            "'" + endIndex + "'"
            ) + " is less than startIndex '" + startIndex + "'");
        }
        return opsTarget.stream().skip(startIndex).limit(endIndex - startIndex).toList();
    }

    @Override
    protected Object operateWithHValue(HValue<List<Object>> opsTarget) {
        return operateWithMutableList(opsTarget.data());
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
            try {
                startIndexOrSupplier = Long.valueOf(token);
                stream().next();
            } catch (NumberFormatException e) {
                startIndexOrSupplier = compileInlineCommand();
                if (startIndexOrSupplier == null) {
                    throw new CommandCompileException("can not parse string '" + token + "' to number." + stream().errToken(token));
                }
            }
            try {
                token = stream().token();
                try {
                    endIndexOrSupplier = Long.valueOf(token);
                    stream().next();
                } catch (NumberFormatException e) {
                    endIndexOrSupplier = compileInlineCommand();
                    if (endIndexOrSupplier == null) {
                        throw new CommandCompileException("can not parse string '" + token + "' to number." + stream().errToken(token));
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }
        }
    }

    @Override
    protected void beforeCompilePipe() {
        if (startIndexOrSupplier == null) {
            throw new CommandCompileException("keyword '" + name() + "' require a start index(integer) to determine the range of list");
        }
    }
}
