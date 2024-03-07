package org.hashdb.ms.persistent.aof;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import org.hashdb.ms.data.Database;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Date: 2024/2/29 13:50
 * <p>
 *
 * @author Huanyu Mark
 */
public class Aof extends ReadableAof implements Closeable, AofFlusher {
    @Getter
    private final Path baseFilePath;
    @Getter
    private final Path newFilePath;
    @Getter
    private final Path dbInfoFilePath;
    @Getter
    private final Path rewriteBaseFilePath;
    @Getter
    private final Path rewriteNewFilePath;
    private final AofFlusher flusher;

    /**
     * @param baseFilePath 基准文件, 可以不存在
     * @param newFilePath  追加文件, 可以不存在
     */
    Aof(
            Path baseFilePath,
            Path newFilePath,
            Path rewriteBaseFilePath,
            Path rewriteNewFilePath,
            Path dbInfoFilePath,
            Database database
    ) throws IOException {
        super(dbInfoFilePath, database);
        this.dbInfoFilePath = dbInfoFilePath;
        this.baseFilePath = baseFilePath;
        this.newFilePath = newFilePath;
        try {
            Files.createFile(this.baseFilePath);
        } catch (FileAlreadyExistsException ignore) {
        }
        try {
            Files.createFile(this.newFilePath);
        } catch (FileAlreadyExistsException ignore) {
        }

        this.rewriteBaseFilePath = rewriteBaseFilePath;
        this.rewriteNewFilePath = rewriteNewFilePath;
        flusher = getAofConfig().getFlushStrategy().newFlusher(this);
    }

    @Override
    public void append(CharSequence command) {
        flusher.append(command);
    }

    @Override
    public void append(ByteBuf commandBuf) {
        flusher.append(commandBuf);
    }

    @Override
    public void append(ByteBuffer commandBuf) {
        flusher.append(commandBuf);
    }

    @Override
    public CompletableFuture<Boolean> flush() {
        return flusher.flush();
    }

    @Override
    protected LineReader newReader() throws IOException {
        try {
            return new BufferedReaderAdaptor(Files.newBufferedReader(baseFilePath, StandardCharsets.UTF_8));
        } catch (FileNotFoundException e) {
            return NullLineReader.get();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            flush().join();
        } finally {
            if (flusher instanceof Closeable closeable) {
                closeable.close();
            }
        }
    }
}
