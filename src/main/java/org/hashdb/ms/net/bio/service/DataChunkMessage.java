package org.hashdb.ms.net.bio.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.net.bio.msg.Message;
import org.hashdb.ms.net.bio.msg.MessageType;
import org.hashdb.ms.net.bio.msg.ServiceMessage;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Date: 2023/12/1 13:15
 *
 * @author huanyuMake-pecdle
 */
@Data
@EqualsAndHashCode(callSuper = true)
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
