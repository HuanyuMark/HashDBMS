package org.hashdb.ms.persistent;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Date: 2024/2/28 13:09
 *
 * @author Huanyu Mark
 */
@Slf4j
public class EachFlusher implements AofFileFlusher, AutoCloseable {
    private final Path target;

    private FileChannel channel;
    private final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();

    public EachFlusher(Path filePath) throws IOException {
        this.target = filePath;
        channel = FileChannel.open(filePath, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }


    @Override
    public void append(CharSequence command) {
        buffer.writeCharSequence(command, StandardCharsets.UTF_8);
        buffer.writeChar('\n');
        flush();
    }

    @Override
    public void append(ByteBuf commandBuf) {
        buffer.writeBytes(commandBuf);
        buffer.writeChar('\n');
        flush();
    }

    @Override
    public void append(ByteBuffer commandBuf) {
        buffer.writeBytes(commandBuf);
        buffer.writeChar('\n');
        flush();
    }

    protected void flush() {
        boolean retry = false;
        while (true) {
            try {
                channel.position(channel.size());
                buffer.readBytes(channel, channel.position(), buffer.readableBytes());
                // 这一步可能会阻塞, 考虑下异步化
                channel.force(true);
                return;
            } catch (IOException e) {
                if (retry) {
                    log.error("can not flush data to AOF file. cause: {}", e.getMessage());
                    return;
                }
                retry = true;
                try {
                    channel.close();
                    channel = FileChannel.open(target, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException ex) {
                    log.error("can not flush data to AOF file. cause: {}", e.getMessage());
                    return;
                }
            } finally {
                buffer.clear();
            }
        }
    }

    @Override
    public void close() {
        buffer.release();
    }
}
