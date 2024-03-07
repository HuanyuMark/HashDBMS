package org.hashdb.ms.net.nio;

import org.hashdb.ms.net.exception.IllegalUpgradeSessionException;
import org.hashdb.ms.net.exception.MaxConnectionException;

import java.util.function.BiFunction;

/**
 * Date: 2024/2/1 17:38
 *
 * @author Huanyu Mark
 */
public enum SessionMeta implements MetaEnum {
    /**
     * 普通的业务使用的Session
     */
    BUSINESS(BusinessConnectionSession.class, (base, this_) -> {
        throw new IllegalUpgradeSessionException(base.getMeta(), this_);
    }),
    /**
     * 管理用的Session, 集群,故障管理等
     */
    MANAGEMENT(ReplicationConnectionSession.class, (base, this_) -> {
        if (!(base instanceof BusinessConnectionSession bus)) {
            throw new IllegalUpgradeSessionException(base.getMeta(), this_);
        }
        return new ReplicationConnectionSession(bus);
    });

    private static final SessionMeta[] ENUM_MAP = SessionMeta.values();

    private final BiFunction<BaseConnectionSession, SessionMeta, BaseConnectionSession> upgrader;

    public static SessionMeta resolve(int sessionMetaKey) {
        return ENUM_MAP[sessionMetaKey];
    }

    private final Class<? extends TransientConnectionSession> sessionClass;

    SessionMeta(Class<? extends TransientConnectionSession> sessionClass, BiFunction<BaseConnectionSession, SessionMeta, BaseConnectionSession> upgrader) {
        this.upgrader = upgrader;
        this.sessionClass = sessionClass;
    }

    public BaseConnectionSession upgradeFrom(BaseConnectionSession base) throws IllegalUpgradeSessionException, MaxConnectionException {
        return upgrader.apply(base, this);
    }

    public Class<? extends TransientConnectionSession> sessionClass() {
        return sessionClass;
    }

    @Override
    public int key() {
        return ordinal();
    }
}
