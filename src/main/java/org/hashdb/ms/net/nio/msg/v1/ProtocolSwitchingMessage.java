package org.hashdb.ms.net.nio.msg.v1;

import org.jetbrains.annotations.Nullable;

/**
 * Date: 2024/1/27 16:11
 * 切换通讯协议
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ProtocolSwitchingMessage extends Message<Integer> {
    public ProtocolSwitchingMessage(long id, @Nullable Integer protocolCode) {
        super(id, protocolCode);
    }

    public ProtocolSwitchingMessage(@Nullable Integer protocolCode) {
        super(protocolCode);
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.PROTOCOL_SWITCHING;
    }
}
