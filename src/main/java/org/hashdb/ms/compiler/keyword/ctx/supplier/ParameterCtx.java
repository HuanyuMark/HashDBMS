package org.hashdb.ms.compiler.keyword.ctx.supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.DatabaseCompileStream;
import org.hashdb.ms.compiler.SupplierCompileStream;
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
 */
// TODO: 2024/1/14 将这个类改造成ValueAccessorCtx, 代理所有“值”(json,parameter,inline command)的访问
// execSupplier()这个方法也改下, 不需要支持复制, 只需要让所有的值访问都是这个Ctx即可,运行时都只会使用全新的值副本
@Slf4j
public class ParameterCtx extends SupplierCtx {

    @Getter
    @JsonProperty
    private String parameterName;

    @JsonIgnore
    private Object valueOrSupplier;
    @JsonProperty
    private DataType storeType;

    private final boolean store;

    private DatabaseCompileStream stream;

    public ParameterCtx(DatabaseCompileStream stream, String parameterName, boolean store) {
        this.parameterName = parameterName;
        this.store = store;
        this.stream = stream;
    }

    @Override
    public SupplierCompileStream stream() {
        throw new UnsupportedOperationException("ParameterCtx can not be stream");
    }

    @Override
    public void setStream(SupplierCompileStream stream) {
        String msg = """
                parameterCtx should be instance directly.
                can not be auto compiled(call by compileWithStream(SupplierCompileStream)).
                """;
        log.warn(msg, new RuntimeException(msg));
        this.stream = stream;
    }

    @Override
    protected Supplier<?> compile() throws StopComplieException {
        // 登记该组流使用该参数, 防止参数变更后缓存的编译流过期
        // 这里登记使用关系的时机， 可能需要改改，比如说放到执行时
        Parameter parameter = stream.session().getParameter(parameterName);
        if (parameter == null) {
            throw new NotFoundParameterException("can not found parameter '" + parameterName + "'." + stream.errToken(parameterName));
        }
        cloneValue(parameter.value());
        // 如果需要存储, 那么就需要克隆, 在命令运行前(编译时)克隆最佳
        if (store) {
            // 在命令被重新运行时重新拷贝一份当前value
            // TODO: 2024/2/5 这里应该不需要注册再运行回调，jsonValue会自动在重运行时克隆更新
            stream.rootStream().onRerun(() -> cloneValue(parameter.value()));
        } else {
            // 在参数值被用户更改时更新当前value
            parameter.onUpdate(v -> valueOrSupplier = analyseValue(v));
        }
        stream.next();
        return executor();
    }

    private Object analyseValue(Object value) {
        // 是普通的json数据
        if (!(value instanceof SupplierCtx v)) {
            if (!store && !(value instanceof String)) {
                throw UnsupportedQueryKey.of(List.of(value), stream.errToken(parameterName));
            }
            storeType = DataType.typeOfRawValue(value);
            return value;
        }
        // 是内联命令
        if (v.storeType().unsupportedClone(v.supplyType())) {
            var supportedDataType = DataType.typeOfCloneable(v.supplyType());
            throw new CommandCompileException(
                    "can not store return type of command '" + v.command() + "'. " +
                            "because the return type can only be converted to " + supportedDataType + "and stored in the database." +
                            stream.errToken(parameterName)
            );
        }
        storeType = v.storeType();
        return v;
    }

    private void cloneValue(Object v) {
        Object value = analyseValue(v);
        // 是内联命令
        if (value instanceof SupplierCtx) {
            valueOrSupplier = value;
            return;
        }
        // 是普通的json数据
        valueOrSupplier = storeType.clone(value);
    }

    @Override
    public Supplier<?> executor() {
        return () -> {
            // 防止多处引用
            Object res = valueOrSupplier instanceof SupplierCtx s ? exeSupplierCtx(s, true) : valueOrSupplier;
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
