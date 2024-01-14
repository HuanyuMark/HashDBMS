package org.hashdb.ms.compiler.keyword.ctx.supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.exception.NotFoundParameterException;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.exception.StopComplieException;
import org.hashdb.ms.exception.UnsupportedQueryKey;
import org.hashdb.ms.net.Parameter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

/**
 * Date: 2024/1/13 22:09
 * 从会话中读取parameter
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
// TODO: 2024/1/14 将这个类改造成ValueAccessorCtx, 代理所有“值”(json,parameter,inline command)的访问
// execSupplier()这个方法也改下, 不需要支持复制, 只需要让所有的值访问都是这个Ctx即可,运行时都只会使用全新的值副本
public class ParameterAccessorCtx extends SupplierCtx {

    @Getter
    @JsonProperty
    private String parameterName;

    @JsonIgnore
    private Object valueOrSupplier;
    @JsonProperty
    private DataType storeType;

    private final boolean store;

    public ParameterAccessorCtx(String parameterName, boolean store) {
        this.parameterName = parameterName;
        this.store = store;
        compile();
    }

    @Override
    protected Supplier<?> compile() throws StopComplieException {
        // 登记该组流使用该参数, 防止参数变更后缓存的编译流过期
        // 这里登记使用关系的时机， 可能需要改改，比如说放到执行时
        Parameter parameter = stream().session().getParameter(parameterName);
        if (parameter == null) {
            throw new NotFoundParameterException("can not found parameter '" + parameterName + "'." + stream().errToken(parameterName));
        }
        updateValue(parameter.value());
        // 在参数值被用户更改时更新当前value
        parameter.onUpdate(this::updateValue);
        // 在命令被重新运行时重新拷贝一份当前value
        if (store) {
            stream().rootStream().onRerun(() -> updateValue(parameter.value()));
        }
        stream().next();
        return executor();
    }

    private void updateValue(Object value) {
        // 是普通的json数据
        if (!(value instanceof SupplierCtx v)) {
            if (!store && !(value instanceof String)) {
                throw UnsupportedQueryKey.of(List.of(value), stream().errToken(parameterName));
            }
            storeType = DataType.typeOfRawValue(value);
            valueOrSupplier = storeType.clone(value);
            return;
        }
        // 是内联命令
        if (v.storeType().unsupportedClone(v.supplyType())) {
            var supportedDataType = DataType.typeOfCloneable(v.supplyType());
            throw new CommandCompileException("can not store return type of command '" + v.command() + "'. " +
                    "because the return type can only be converted to " + supportedDataType + "and stored in the database." +
                    stream().errToken(parameterName));
        }
        storeType = v.storeType();
        valueOrSupplier = v;
    }

    @Override
    public Supplier<?> executor() {
        return () -> {
            Object res = valueOrSupplier instanceof SupplierCtx s ? exeSupplierCtx(s) : valueOrSupplier;
            // 防止多处引用
            valueOrSupplier = null;
            return res;
        };
    }

    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.$$PARAMETER_ACCESS$$;
    }

    public Object value() {
        return valueOrSupplier;
    }

    @Override
    public @NotNull DataType storeType() {
        return storeType;
    }

    @Override
    public @NotNull Class<?> supplyType() {
        return valueOrSupplier instanceof SupplierCtx s ? s.supplyType() : storeType.reflect().clazz();
    }
}
