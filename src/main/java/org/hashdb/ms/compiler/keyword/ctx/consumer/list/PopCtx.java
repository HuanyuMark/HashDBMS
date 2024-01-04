package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.ConsumerCompileStream;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.compiler.option.DeleteOpCtx;
import org.hashdb.ms.compiler.option.HDeleteOpCtx;
import org.hashdb.ms.compiler.option.LDeleteOpCtx;
import org.hashdb.ms.compiler.option.OptionCtx;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTaskPriority;
import org.hashdb.ms.data.task.ImmutableChecker;
import org.hashdb.ms.exception.CommandCompileException;

import java.util.List;
import java.util.function.Function;

/**
 * Date: 2023/11/29 10:05
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class PopCtx extends MutableListCtx {

    @Override
    public void setStream(ConsumerCompileStream stream) {
        super.setStream(stream);
        stream().toWrite();
    }

    protected Boolean delete;

    protected int popCount = 1;

    protected SupplierCtx popCountSupplier;

    protected OpsTaskPriority deletePriority;

    protected PopCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    public Class<?> supplyType() {
        return ImmutableChecker.unmodifiableList;
    }

    @Override
    protected void beforeCompile() {
        doCompile();
    }

    @Override
    protected Object operateWithMutableList(List<Object> opsTarget) {
        return doPop(opsTarget);
    }

    @Override
    protected Object operateWithHValue(HValue<List<Object>> opsTarget) {
        return operateWithMutableList(opsTarget.data());
    }

    private void doCompile() {
        while (true) {
            String token;
            try {
                try {
                    compilePipe();
                } catch (ArrayIndexOutOfBoundsException e) {
                    return;
                }
                filterAllKeywords();
                token = stream().token();
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }
            Function<OptionCtx<?>, Boolean> opFilter = op -> {
                if (op instanceof DeleteOpCtx deleteOpCtx) {
                    delete = deleteOpCtx.value();
                } else if (op instanceof HDeleteOpCtx deleteOpCtx) {
                    delete = deleteOpCtx.value();
                    deletePriority = OpsTaskPriority.HIGH;
                } else if (op instanceof LDeleteOpCtx deleteOpCtx) {
                    delete = deleteOpCtx.value();
                    deletePriority = OpsTaskPriority.LOW;
                }
                addOption(op);
                return true;
            };
            try {
                popCount = Integer.parseInt(token);
                stream().next();
            } catch (NumberFormatException e) {
                try {
                    popCountSupplier = compileInlineCommand();
                } catch (ArrayIndexOutOfBoundsException ex) {
                    return;
                }
                if (popCountSupplier == null) {
                    throw new CommandCompileException("can not parse string '" + token + "'to integer." + stream().errToken(token));
                }
            }
            try {
                compileOptions(opFilter);
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }
        }
    }


    abstract List<Object> doPop(List<Object> opsTarget);
}
