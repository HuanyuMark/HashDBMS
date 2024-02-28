package org.hashdb.ms.persistent;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.util.AsyncService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;

/**
 * Date: 2024/2/28 13:29
 *
 * @author Huanyu Mark
 */
@Slf4j
public class IntervalFlusher implements AofFileFlusher, AutoCloseable {
    @Getter
    private final long interval;
    private final Path target;

    private FileChannel channel;
    private final Queue<Object> cache = new LinkedBlockingQueue<>();

    private final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();

    private final ScheduledFuture<?> flusherTask;

    public IntervalFlusher(Path filePath, long msInterval) throws IOException {
        this.target = filePath;
        channel = FileChannel.open(filePath, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.APPEND, StandardOpenOption.APPEND);
        this.interval = msInterval;
        flusherTask = AsyncService.setInterval(() -> {
            var command = cache.poll();
            if (command == null) {
                if (buffer.readableBytes() > 0) {
                    flush();
                }
                return;
            }
            switch (command) {
                case CharSequence sequence -> buffer.writeCharSequence(sequence, StandardCharsets.UTF_8);
                case ByteBuf buf -> buffer.writeBytes(buf);
                case ByteBuffer buf -> buffer.writeBytes(buf);
                default -> {
                }
            }
        }, msInterval);
    }

    @Override
    public void close() {
        buffer.release();
        flusherTask.cancel(true);
    }

    @Override
    public void append(CharSequence command) {
        cache.add(command);
    }

    @Override
    public void append(ByteBuf commandBuf) {
        cache.add(commandBuf);
    }

    @Override
    public void append(ByteBuffer commandBuf) {
        cache.add(commandBuf);
    }

    private void flush() {
        boolean retry = false;
        while (true) {
            try {
                channel.position(channel.size());
                buffer.readBytes(channel, channel.position(), buffer.readableBytes());
                buffer.clear();
                return;
            } catch (IOException e) {
                if (retry) {
                    log.error("can not flush data to AOF file. cause: {}", e.getMessage());
                    buffer.clear();
                    return;
                }
                retry = true;
                try {
                    channel = FileChannel.open(target, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.APPEND);
                } catch (IOException ex) {
                    log.error("can not flush data to AOF file. cause: {}", ex.getMessage());
                    buffer.clear();
                    return;
                }
            }
        }
    }
}
