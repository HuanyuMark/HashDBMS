package org.hashdb.ms.net;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.compiler.CompileStream;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.manager.DBSystem;
import org.hashdb.ms.util.CacheMap;
import org.hashdb.ms.util.Lazy;

import java.util.HashMap;
import java.util.Map;

/**
 * Date: 2023/12/6 11:24
 * 用来做网络传输的 ConnectionSession, 比如, 要告诉主机
 * 执行写命令的数据库是哪个
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Data
public class TransportableConnectionSession implements ConnectionSessionModel {
    private static final Lazy<DBSystem> SYSTEM = Lazy.of(() -> HashDBMSApp.ctx().getBean(DBSystem.class));

    //    private static final
    @JsonProperty
    private Integer dbId;
    @JsonIgnore
    private Database database;
    private Long commandCacheAliveTime;
    private Integer cacheSize;
    private CacheMap<String, CompileStream<?>> localCommandCache;
    /**
     * 参数名以'$'开头
     * 参数名-{@link org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx}
     * 参数名-{@link org.hashdb.ms.data.DataType} 里支持的数据类型的java对象实例
     */
    private Map<String, Parameter> parameters;

    /**
     * 给json序列化预留的构造器, 不能主动调用
     */
    public TransportableConnectionSession() {
    }

    /**
     * 需要主动调用这个构造器来传输 {@link ConnectionSession}
     */
    public TransportableConnectionSession(ConnectionSession session) {
        Database database = session.getDatabase();
        if (database != null) {
            dbId = database.getInfos().getId();
        }
        commandCacheAliveTime = session.getLocalCommandCache().getAliveTime();
    }

    @Override
    public Database getDatabase() {
        return database != null ? database : SYSTEM.get().getDatabase(dbId);
    }

    @Override
    public synchronized Parameter setParameter(String name, Object value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        Parameter oldValue;
        if (value == null) {
            oldValue = parameters.remove(name);
        } else {
            oldValue = parameters.put(name, new Parameter(value));
        }
        if (oldValue != null) {
            oldValue.usedCacheCommands.parallelStream().forEach(localCommandCache::remove);
        }
        return null;
    }

    @Override
    public Parameter getParameter(String name) {
        if (parameters == null) {
            return null;
        }
        return parameters.get(name);
    }

    @Override
    public void useParameter(Parameter parameter, String command) {
        parameter.usedCacheCommands.add(command);
    }

    public long getCommandCacheAliveTime() {
        return commandCacheAliveTime;
    }

    public void setCommandCacheAliveTime(Long commandCacheAliveTime) {
        this.commandCacheAliveTime = commandCacheAliveTime;
        if (commandCacheAliveTime != null && cacheSize != null) {
            localCommandCache = new CacheMap<>(commandCacheAliveTime, cacheSize);
        }
    }

    public Integer getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(Integer cacheSize) {
        this.cacheSize = cacheSize;
        if (commandCacheAliveTime != null && cacheSize != null) {
            localCommandCache = new CacheMap<>(commandCacheAliveTime, cacheSize);
        }
    }
}
