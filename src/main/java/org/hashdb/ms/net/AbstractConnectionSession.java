package org.hashdb.ms.net;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.hashdb.ms.compiler.CompileStream;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.util.CacheMap;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Date: 2024/1/13 21:49
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class AbstractConnectionSession implements ConnectionSessionModel, AutoCloseable {
    /**
     * 与参数相关的命令都缓存在这里
     */
    @Getter
    protected CacheMap<String, CompileStream<?>> localCommandCache;

    @Nullable
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    protected Database database;
    /**
     * 参数名以'$'开头
     * 参数名-{@link org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx}
     * 参数名-{@link org.hashdb.ms.data.DataType} 里支持的数据类型的java对象实例
     */
    protected Map<String, Parameter> parameters;

    @Override
    public @Nullable Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        if (database == null) {
            close();
        } else {
            database.use();
        }
        this.database = database;
    }

    @Override
    public synchronized Parameter setParameter(String name, Object value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        Parameter oldValue;
        if (value == null) {
            return parameters.remove(name);
        }
        Parameter parameter = parameters.get(name);
        if (parameter != null) {
            // 通知所有使用了该参数的命令更新参数值
            parameter.notifyUpdate(value);
            return parameter;
        }
        Parameter newParameter = new Parameter(value);
        parameters.put(name, new Parameter(value));
        return newParameter;
    }

    @Override
    public Parameter getParameter(String name) {
        if (parameters == null) {
            return null;
        }
        return parameters.get(name);
    }

    @Override
    public void close() {
        if (database != null) {
            database.release();
        }
    }
}
