package org.hashdb.ms.net;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.manager.DBSystem;
import org.hashdb.ms.util.Lazy;

/**
 * Date: 2023/12/6 11:24
 * 用来做网络传输的 ConnectionSession, 比如, 要告诉主机
 * 执行写命令的数据库是哪个
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Data
public class TransportableConnectionSession implements ReadonlyConnectionSession {
    private static final Lazy<DBSystem> SYSTEM = Lazy.of(() -> HashDBMSApp.ctx().getBean(DBSystem.class));
    @JsonProperty
    private int dbId;

    @JsonIgnore
    private Database database;

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
        dbId = database.getInfos().getId();
    }

    @Override
    public Database getDatabase() {
        return database != null ? database : SYSTEM.get().getDatabase(dbId);
    }
}
