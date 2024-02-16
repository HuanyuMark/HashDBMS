package org.hashdb.ms.net;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.config.CommandCacheConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.manager.DBSystem;
import org.hashdb.ms.net.bio.BIOConnectionSession;
import org.hashdb.ms.util.CacheMap;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;

/**
 * Date: 2023/12/6 11:24
 * 用来做网络传输的 ConnectionSession, 比如, 要告诉主机
 * 执行写命令的数据库是哪个
 *
 * @author huanyuMake-pecdle
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TransportableConnectionSession extends AbstractConnectionSession implements ConnectionSession {
    private static final Lazy<DBSystem> SYSTEM = Lazy.of(() -> HashDBMSApp.ctx().getBean(DBSystem.class));

    //    private static final
    @JsonProperty
    private Integer dbId;

    @JsonProperty(required = true)
    private CommandCacheConfig commandCacheConfig;

    /**
     * 给json序列化预留的构造器, 不能主动调用
     */
    @JsonCreator
    public TransportableConnectionSession() {
    }

    /**
     * 需要主动调用这个构造器来传输 {@link BIOConnectionSession}
     */
    public TransportableConnectionSession(BIOConnectionSession session, CommandCacheConfig config) {
        Database database = session.getDatabase();
        if (database != null) {
            dbId = database.getInfos().getId();
        }
        commandCacheConfig = config;
    }

    @Override
    public Database getDatabase() {
        if (dbId == null) {
            return null;
        }
        if (super.getDatabase() != null) {
            return super.getDatabase();
        }
        Database db = SYSTEM.get().getDatabase(dbId);
        setDatabase(db);
        return db;
    }

    public void setCommandCacheConfig(@NotNull CommandCacheConfig config) {
        this.commandCacheConfig = config;
        localCommandCache = new CacheMap<>(config.getAliveDuration(), config.getCacheSize());
    }

    public CommandCacheConfig getCommandCacheConfig() {
        return commandCacheConfig;
    }
}
