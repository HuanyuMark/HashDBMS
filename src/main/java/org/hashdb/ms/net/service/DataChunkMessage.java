package org.hashdb.ms.net.service;

import lombok.Getter;
import org.hashdb.ms.net.msg.Message;
import org.hashdb.ms.net.msg.MessageType;
import org.hashdb.ms.net.msg.ServiceMessage;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Date: 2023/12/1 13:15
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Getter
public class DataChunkMessage extends ServiceMessage {

    private final UUID ownerId;

    private final int chunkIndex;

    public DataChunkMessage(@NotNull Message owner, int chunkIndex, String data) {
        this.ownerId = owner.getId();
        this.chunkIndex = chunkIndex;
        setData(data);
    }
    @Override
    public MessageType getType() {
        return MessageType.DATA_CHUNK;
    }
}
