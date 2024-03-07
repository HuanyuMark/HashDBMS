package org.hashdb.ms.persistent.info;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.hashdb.ms.data.DatabaseInfos;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * Date: 2024/3/6 0:46
 *
 * @author Huanyu Mark
 */
public record HashProtocolV1ByteBufDbInfoBroker(ByteBuf buffer,
                                                Charset charset) implements DbInfoBroker, AutoCloseable {

    @Override
    public DatabaseInfos readInfos() throws IOException {
        int id = buffer.readInt();
        int dbNameByteLength = buffer.readInt();
        var dbName = buffer.readCharSequence(dbNameByteLength, charset).toString();
        long createTime = buffer.readLong();
        long lastSaveTime = buffer.readLong();
        var infos = new DatabaseInfos(id, dbName, new Date(createTime));
        infos.setLastSaveTime(new Date(lastSaveTime));
        return infos;
    }

    @Override
    public void writeInfos(DatabaseInfos infos) throws IOException {
        int id = infos.getId();
        var dbName = infos.getName();
        var createTime = infos.getCreateTime();
        var buffer = ByteBufAllocator.DEFAULT.buffer();
        var dbNameBytes = dbName.getBytes(charset);
        // id
        buffer.writeInt(id);
        // db name bytes length
        buffer.writeInt(dbNameBytes.length);
        // db name bytes
        buffer.writeBytes(dbNameBytes);
        // create time
        buffer.writeLong(createTime.getTime());
        // last save time
        buffer.writeLong(System.currentTimeMillis());
    }

    @Override
    public void close() {
        buffer.release();
    }
}
