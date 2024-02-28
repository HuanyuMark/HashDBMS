package org.hashdb.ms.persistent;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.AofConfig;
import org.hashdb.ms.support.Exit;
import org.hashdb.ms.support.StaticAutowired;
import org.hashdb.ms.util.AsyncService;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Date: 2024/1/7 0:34
 * 保存方式:
 * 同步的
 * 按保存时间间隔分, 有每次every-time, every-second, no
 * {@link #append} 方法与 {@link #flush()} 方法是同步的
 *
 * @author Huanyu Mark
 */
@Slf4j
public abstract class AofFile implements AutoCloseable {

    @Getter
    @StaticAutowired
    private static AofConfig aofConfig;

    /**
     * @param content 这个文件不一定需要存在, 只要 {@link #buffer} 不为空,
     *                且调用过 {@link #flush()} 持久化到硬盘上, 这个文件就会被创建
     * @param order   这个AofFile在所有AofFile中的次序
     */
    public static FileSystemAofFile create(File content, int order) {
        return new FileSystemAofFile(content, order);
    }

    public static AofFile createReadonly(ByteBuf content) {
        return new ReadonlyBufferAofFile(content);
    }

    //    protected final AofFileFlusher flusher;
    protected final Queue<Object> cache = new LinkedBlockingQueue<>();

    protected final ByteBuf buffer;

    protected AofFile() {
        buffer = ByteBufAllocator.DEFAULT.buffer();
    }

    protected AofFile(ByteBuf buffer) {
        this.buffer = buffer;
    }

    public boolean hasCache() {
        return buffer.readerIndex() < buffer.writerIndex();
    }

    public synchronized void append(CharSequence command) {
        buffer.writeCharSequence(command, StandardCharsets.UTF_8);
    }

    public synchronized void append(ByteBuf commandBuf) {
        buffer.writeBytes(commandBuf);
    }

    public synchronized void append(ByteBuffer commandBuf) {
        buffer.writeBytes(commandBuf);
    }

    /**
     * 读写互斥, 不能同时进行
     */
    public void flush() {
    }

    public LineReader readAll() {
        try {
            return newReader();
        } catch (IOException e) {
            throw Exit.error("can not read aof file", e);
        }
    }

    public void readAllAsync(Consumer<String> commandConsumer) {
        AsyncService.start(() -> {
            try (var reader = newReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    commandConsumer.accept(line);
                }
            } catch (IOException e) {
                throw Exit.error("can not read aof file", e);
            }
        });
    }

    protected abstract LineReader newReader() throws IOException;

    @Override
    public void close() throws IOException {
        buffer.release();
    }

    public static class FileSystemAofFile extends AofFile {

        private final Path path;

        private final FileChannel fileChannel;
        /**
         * 这个AofFile在所有AofFile中的次序
         */
        private final int order;

        /**
         * @param content 文件必须存在
         * @param order   这个AofFile在所有AofFile中的次序
         */
        FileSystemAofFile(File content, int order) {
            this.path = content.toPath();
            this.order = order;
            try {
                fileChannel = FileChannel.open(path, StandardOpenOption.APPEND, StandardOpenOption.READ, StandardOpenOption.WRITE);
            } catch (IOException e) {
                throw Exit.error(STR."can not open aof file '\{content}'", e);
            }
        }

        @Override
        public synchronized void flush() {
            if (!hasCache()) {
                return;
            }
            try {
                fileChannel.position(fileChannel.size());
                buffer.readBytes(fileChannel, fileChannel.position(), buffer.readableBytes());
            } catch (IOException e) {
                throw Exit.error(STR."can not flush aof file '\{path}'", e);
            }
            buffer.clear();
        }

        @Override
        protected LineReader newReader() throws IOException {
            try {
                fileChannel.position(0);
                return new BufferedReaderLineAdaptor(Files.newBufferedReader(path, StandardCharsets.UTF_8));
            } catch (FileNotFoundException e) {
                return new NullLineReader();
            }
        }

        public File file() {
            return path.toFile();
        }

        public int order() {
            return order;
        }

        @Override
        public void close() throws IOException {
            super.close();
            fileChannel.close();
        }
    }

    public static class ReadonlyBufferAofFile extends AofFile {

        private final int readerIndex;

        public ReadonlyBufferAofFile(ByteBuf buffer) {
            super(buffer);
            readerIndex = buffer.readerIndex();
        }

        @Override
        public void append(CharSequence command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void append(ByteBuf commandBuf) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void append(ByteBuffer commandBuf) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void flush() {
            throw new UnsupportedOperationException();
        }

        @Override
        public LineReader newReader() {
            buffer.readerIndex(readerIndex);
            return new BufferedReaderLineAdaptor(new BufferedReader(new InputStreamReader(new ByteBufInputStream(buffer), StandardCharsets.UTF_8)));
        }
    }

    static class BufferedReaderLineAdaptor implements LineReader {

        private final BufferedReader reader;

        public BufferedReaderLineAdaptor(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public String readLine() throws IOException {
            return reader.readLine();
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }

    static class NullLineReader implements LineReader {
        @Override
        public @Nullable String readLine() throws IOException {
            return null;
        }

        @Override
        public void close() throws IOException {

        }
    }
}
