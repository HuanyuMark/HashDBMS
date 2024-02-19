package org.hashdb.ms.net.nio;

/**
 * Date: 2024/2/1 17:38
 *
 * @author huanyuMake-pecdle
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

    private static final SessionMeta[] SESSION_META_MAP = SessionMeta.values();

    public static SessionMeta resolve(int sessionMetaKey) {
        return SESSION_META_MAP[sessionMetaKey];
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
