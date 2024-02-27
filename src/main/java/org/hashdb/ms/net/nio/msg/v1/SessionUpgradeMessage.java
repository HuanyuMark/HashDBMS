package org.hashdb.ms.net.nio.msg.v1;

import org.hashdb.ms.net.nio.SessionMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Date: 2024/2/16 19:56
 *
 * @author huanyuMake-pecdle
 */
public class SessionUpgradeMessage extends Message<SessionMeta> {

    public static final SessionUpgradeMessage DEFAULT = new SessionUpgradeMessage(0, SessionMeta.BUSINESS.key());

    public SessionUpgradeMessage(long id, int sessionMetaCode) {
        super(id, SessionMeta.resolve(sessionMetaCode));
    }

    public SessionUpgradeMessage(SessionMeta body) {
        super(body);
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.SESSION_UPGRADE;
    }

    /**
     * target session
     * 要升级到该 {@link SessionMeta}的meta
     */
    @Override
    public @NotNull SessionMeta body() {
        return super.body();
    }
}
