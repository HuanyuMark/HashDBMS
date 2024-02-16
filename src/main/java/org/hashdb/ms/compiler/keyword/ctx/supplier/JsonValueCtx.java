package org.hashdb.ms.compiler.keyword.ctx.supplier;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.exception.StopComplieException;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Date: 2024/1/14 21:14
 * 为了支持命令缓存，有必要提供一个编译上下文结点，在命令重新运行时
 * 克隆一遍json对象，否则命令重新运行时，会读写重复的对象(引用相同)
 * 导致读写的含义混乱(可读可写对象不能有多处引用)
 *
 * @author huanyuMake-pecdle
 */
public class JsonValueCtx extends SupplierCtx {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private final Object value;

    private Object toStore;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private final DataType storeType;

    @JsonCreator
    public JsonValueCtx(@JsonProperty DataType storeType, @JsonProperty Object value) {
        this.value = value;
        this.storeType = storeType;
    }

    @Override
    protected Supplier<?> compile() throws StopComplieException {
        stream().rootStream().onRerun(this::copy);
        return executor();
    }

    protected void copy() {
        toStore = storeType.clone(value);
    }

    @Override
    public Supplier<?> executor() {
        copy();
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
