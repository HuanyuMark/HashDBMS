package org.hashdb.ms.net.nio.msg.v1;

import org.hashdb.ms.net.nio.SessionMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Date: 2024/2/1 16:57
 * todo 使用 {@link SessionUpgradeMessage} 来替换, 现在仅存在SessionUpgrade需求
 *
 * @author huanyuMake-pecdle
 */
@Deprecated
public class SessionSwitchingMessage extends Message<SessionMeta> {

    public static final SessionSwitchingMessage DEFAULT = new SessionSwitchingMessage(0, SessionMeta.BUSINESS.key());

    public SessionSwitchingMessage(long id, int sessionMetaCode) {
        super(id, SessionMeta.resolve(sessionMetaCode));
    }

    public SessionSwitchingMessage(SessionMeta body) {
        super(body);
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.SESSION_SWITCHING;
    }

    @Override
    public @NotNull SessionMeta body() {
        return super.body();
    }
}
