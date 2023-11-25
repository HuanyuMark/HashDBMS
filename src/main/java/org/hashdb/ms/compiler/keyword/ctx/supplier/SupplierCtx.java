package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.keyword.ctx.CmdCtx;
import org.hashdb.ms.compiler.option.OptionContext;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.exception.DBInnerException;
import org.hashdb.ms.exception.StopComplieException;

import java.util.HashMap;
import java.util.Map;

/**
 * Date: 2023/11/24 16:37
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class SupplierCtx extends CmdCtx<SupplierCompileStream> {

    private OpsTask<?> compileResult;

    public OpsTask<?> doAfterCompile(SupplierCompileStream compileStream) throws StopComplieException {
        if (compileResult != null) {
            throw new DBInnerException(getClass().getSimpleName() + " is finish compilation");
        }
        stream = compileStream;
        this.compileResult = compile(compileStream);
        return compileResult;
    }

    abstract protected OpsTask<?> compile(SupplierCompileStream compileStream) throws StopComplieException;

    protected SupplierCtx(Map<Class<? extends OptionContext<?>>, OptionContext<?>> initialOptions) {
        super(initialOptions);
    }

    protected SupplierCtx() {
        super(new HashMap<>());
    }

    public OpsTask<?> compileResult() {
        return compileResult;
    }

    @Override
    public Class<?> supplyType() {
        return Object.class;
    }
}
