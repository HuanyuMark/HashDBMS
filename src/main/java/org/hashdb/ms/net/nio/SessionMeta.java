package org.hashdb.ms.net.nio;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Date: 2024/2/1 17:38
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public enum SessionMeta implements MetaEnum {
    /**
     * 普通的业务使用的Session
     */
    BUSINESS(BusinessConnectionSession.class),
    /**
     * 管理用的Session, 集群,故障管理等
     */
    MANAGEMENT(ManageConnectionSession.class);

    private static final SessionMeta[] MAP = SessionMeta.values();

    private static final AtomicLong idGenerator = new AtomicLong(0);

    public static long nextId() {
        return idGenerator.incrementAndGet();
    }

    public static SessionMeta resolve(int sessionMetaKey) {
        return MAP[sessionMetaKey];
    }

    private final Class<? extends TransientConnectionSession> sessionClass;

    SessionMeta(Class<? extends TransientConnectionSession> sessionClass) {
        this.sessionClass = sessionClass;
    }

    public Class<? extends TransientConnectionSession> sessionClass() {
        return sessionClass;
    }

    @Override
    public int key() {
        return ordinal();
    }
}
