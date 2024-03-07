package org.hashdb.ms.net.nio.msg.v1;

import org.jetbrains.annotations.Nullable;

/**
 * Date: 2024/3/6 21:27
 *
 * @author Huanyu Mark
 */
public class SyncMessage extends Message<SyncMessage.Body> {
    public record Body(long offset, CharSequence command) {
    }

    public SyncMessage(int id, @Nullable SyncMessage.Body body) {
        super(id, body);
    }

    public SyncMessage(@Nullable SyncMessage.Body body) {
        super(body);
    }

    public SyncMessage(long offset, CharSequence command) {
        this(new Body(offset, command));
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.SYNC;
    }
}
