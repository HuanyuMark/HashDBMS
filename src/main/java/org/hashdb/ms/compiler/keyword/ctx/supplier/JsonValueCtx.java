package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.exception.StopComplieException;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Date: 2024/1/14 21:14
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class JsonValueCtx extends SupplierCtx {

    private final Object value;

    private Object toStore;
    private final DataType storeType;

    public JsonValueCtx(DataType storeType, Object value) {
        this.value = value;
        this.storeType = storeType;
    }

    @Override
    protected Supplier<?> compile() throws StopComplieException {
        copy();
        stream().rootStream().onRerun(this::copy);
        return executor();
    }

    protected void copy() {
        toStore = storeType.clone(value);
    }

    @Override
    public Supplier<?> executor() {
        return () -> {
            var res = toStore;
            toStore = null;
            return res;
        };
    }

    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.$$VALUE$$;
    }

    @Override
    public @NotNull DataType storeType() {
        return storeType;
    }
}
