package org.hashdb.ms.net.nio.msg.v1;

import org.hashdb.ms.net.nio.ServerNode;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2024/2/19 22:18
 *
 * @author huanyuMake-pecdle
 */
public class ServerNodeStateMessage extends Message<ServerNode> {
    public ServerNodeStateMessage(long id, @Nullable ServerNode body) {
        super(id, body);
    }

    public ServerNodeStateMessage(@Nullable ServerNode body) {
        super(body);
    }

    @Override
    public MessageMeta getMeta() {
        return null;
    }
}
