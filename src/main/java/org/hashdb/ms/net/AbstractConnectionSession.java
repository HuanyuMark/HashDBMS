package org.hashdb.ms.net;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.hashdb.ms.compiler.CompileStream;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.net.client.CloseMessage;
import org.hashdb.ms.util.CacheMap;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2024/1/13 21:49
 *
 * @author huanyuMake-pecdle
 */
public abstract class AbstractConnectionSession implements ConnectionSession {
    /**
     * 与参数相关的命令都缓存在这里
     */
    @Getter
    protected CacheMap<String, CompileStream<?>> localCommandCache;

    @Nullable
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Database database;
    /**
     * 参数名以'$'开头
     * 参数名-{@link org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx}
     * 参数名-{@link org.hashdb.ms.data.DataType} 里支持的数据类型的java对象实例
     */
    protected CacheMap<String, Parameter> parameters;
    protected volatile boolean closed = false;

    @Override
    public @Nullable Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        if (database == null) {
            close();
        } else {
            database.retain();
        }
        this.database = database;
    }

    /**
     * @param name  '$'开头的参数名
     * @param value {@link org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx} 内联命令或者是
     *              {@link org.hashdb.ms.data.DataType} 支持的一个java类实例
     *              如果为null,则意为删除
     */
    @Override
    public Parameter setParameter(String name, Object value) {
        if (parameters == null) {
            parameters = new CacheMap<>();
        }
        if (value == null) {
            return parameters.expire(name);
        }
        var parameter = parameters.hit(name);
        if (parameter != null) {
            // 通知所有使用了该参数的命令更新参数值
            parameter.notifyUpdate(value);
            return parameter;
        }
        return parameters.save(name, new Parameter(value));
    }

    @Override
    public Parameter getParameter(String name) {
        if (parameters == null) {
            return null;
        }
        return parameters.hit(name);
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        doClose();
        closed = true;
    }

    protected void doClose() {
        if (database != null) {
            database.release();
        }
    }

    @Override
    public synchronized void close(CloseMessage closeMessage) {
        if (closed) {
            return;
        }
        doClose(null);
    }

    protected void doClose(CloseMessage message) {
        doClose();
    }

}
