package org.hashdb.ms.persistent.aof;

import com.sun.nio.file.ExtendedOpenOption;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.AofConfig;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.exception.IOExceptionWrapper;
import org.hashdb.ms.support.CompletableFuturePool;
import org.hashdb.ms.support.StaticAutowired;
import org.hashdb.ms.support.SystemCall;
import org.hashdb.ms.util.AsyncService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

/**
 * Date: 2024/3/3 15:27
 *
 * @author Huanyu Mark
 */
@Slf4j
public abstract class AbstractAofFlusher implements AofFlusher {

    @Getter
    @StaticAutowired(required = false)
    private static AofConfig aofConfig;

    protected final Aof aof;

    private long baseFileSize;

    private long newFileSize;

    protected final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();

    private FileChannel newFileChannel;

    private final Charset charset = StandardCharsets.UTF_8;

    public AbstractAofFlusher(Aof aof) throws IOException {
        this.aof = aof;
        try {
            newFileChannel = FileChannel.open(aof.getNewFilePath(), StandardOpenOption.WRITE,
                    StandardOpenOption.READ,
                    StandardOpenOption.APPEND,
                    ExtendedOpenOption.DIRECT);
        } catch (UnsupportedOperationException e) {
            newFileChannel = FileChannel.open(aof.getNewFilePath(), StandardOpenOption.WRITE,
                    StandardOpenOption.READ,
                    StandardOpenOption.APPEND);
        }
    }

    @Override
    public void append(CharSequence command) {
        writerToBuffer(command);
        flush();
    }

    @Override
    public void append(ByteBuf commandBuf) {
        writerToBuffer(commandBuf);
        flush();
    }

    @Override
    public void append(ByteBuffer commandBuf) {
        writerToBuffer(commandBuf);
        flush();
    }

    /**
     * @return <strong>物理文件</strong>是否到达重写条件
     */
    protected boolean distFileRewritable() {
        var rewriteConfig = aofConfig.getRewrite();
        return baseFileSize > rewriteConfig.getMinSize() && newFileSize > baseFileSize * rewriteConfig.getPercentage();
    }

    /**
     * @return 同步方法
     */
    protected boolean doFlush() {
        boolean retry = false;
        while (true) {
            try {
                newFileChannel.position(newFileChannel.size());
                buffer.readBytes(newFileChannel, newFileChannel.position(), buffer.readableBytes());
                buffer.clear();
                return true;
            } catch (IOException e) {
                if (retry) {
                    log.error("can not flush data to AOF file. cause: {}", e.getMessage());
                    buffer.clear();
                    return false;
                }
                retry = true;
                try {
                    newFileChannel = FileChannel.open(aof.getNewFilePath(), StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.APPEND);
                } catch (IOException ex) {
                    log.error("can not flush data to AOF file. cause: {}", ex.getMessage());
                    buffer.clear();
                    return false;
                }
            }
        }
    }

    protected void forceFlush() {
        if (newFileChannel != null && newFileChannel.isOpen()) {
            try {
                newFileChannel.force(true);
                newFileSize = aof.getNewFilePath().toFile().length();
            } catch (IOException e) {
                log.error("can not flush data to AOF file. cause: {}", e.getMessage());
            }
        }
    }

    @Override
    public CompletableFuture<Boolean> flush() {
        if (doFlush()) {
            forceFlush();
            if (distFileRewritable()) {
                return doRewrite();
            }
        }
        return CompletableFuturePool.get(false);
    }

    /**
     * 异步方法
     */
    protected synchronized CompletableFuture<Boolean> doRewrite() {
        if (aof.getDatabase() == null) {
            return CompletableFuturePool.get(true);
        }
        try {
            if (newFileChannel != null && newFileChannel.isOpen()) {
                newFileChannel.close();
            }
            newFileChannel = FileChannel.open(aof.getRewriteNewFilePath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE);
        } catch (IOException e) {
            log.error("can not rewrite AOF. cause '{}'", e.getMessage());
            return CompletableFuturePool.get(false);
        }
        var future = SystemCall.forkRun(() -> {
            try (var rewriteChannel = FileChannel.open(aof.getRewriteBaseFilePath(), StandardOpenOption.APPEND, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
                 var lock = rewriteChannel.lock();
            ) {
                rewriteChannel.position(rewriteChannel.size());
                var buf = ByteBufAllocator.DEFAULT.buffer();
                for (HValue<?> hValue : aof.getDatabase()) {
                    hValue.writeCommand(buf);
                    buf.writeChar('\n');
                }
                buf.readBytes(rewriteChannel, rewriteChannel.position(), buf.readableBytes());
                buf.release();
                try {
                    Files.move(aof.getRewriteBaseFilePath(), aof.getBaseFilePath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(aof.getRewriteBaseFilePath(), aof.getBaseFilePath(), StandardCopyOption.REPLACE_EXISTING);
                }
                rewriteChannel.force(true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return future.exceptionallyAsync(e -> {
                    log.error("can not rewrite AOF. cause '{}'", e.getMessage());
                    return false;
                }, AsyncService.service())
                .thenApplyAsync(this::replaceNewFileAndCacheSizeData, AsyncService.service());
    }

    @NotNull
    private Boolean replaceNewFileAndCacheSizeData(Boolean ok) {
        if (!ok) {
            return false;
        }
        synchronized (this) {
            // cache size data
            baseFileSize = aof.getBaseFilePath().toFile().length();
            // 在重写期间,没有新的命令被刷入硬盘
            if (!Files.exists(aof.getRewriteNewFilePath())) {
                try {
                    if (newFileChannel != null && newFileChannel.isOpen()) {
                        newFileChannel.close();
                    }
                    newFileChannel = FileChannel.open(aof.getNewFilePath(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.READ, StandardOpenOption.WRITE);
                    newFileSize = 0;
                    return true;
                } catch (IOException e) {
                    throw new IOExceptionWrapper(e);
                }
            }
            try {
                if (newFileChannel != null && newFileChannel.isOpen()) {
                    newFileChannel.close();
                }
                Files.move(aof.getRewriteNewFilePath(), aof.getNewFilePath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                try {
                    Files.move(aof.getRewriteNewFilePath(), aof.getNewFilePath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    throw new IOExceptionWrapper(ex);
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
            try {
                newFileChannel = FileChannel.open(aof.getNewFilePath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
                newFileSize = aof.getNewFilePath().toFile().length();
                return true;
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }
    }

    protected void writerToBuffer(Object command) {
        switch (command) {
            case CharSequence sequence -> writerToBuffer(sequence);
            case ByteBuf buf -> writerToBuffer(buf);
            case ByteBuffer buf -> writerToBuffer(buf);
            default -> {
            }
        }
    }

    protected void writerToBuffer(CharSequence command) {
        buffer.writeCharSequence(command, charset);
        buffer.writeChar('\n');
    }

    protected void writerToBuffer(ByteBuf command) {
        buffer.writeBytes(command);
        buffer.writeChar('\n');
    }

    protected void writerToBuffer(ByteBuffer command) {
        buffer.writeBytes(command);
        buffer.writeChar('\n');
    }

    @Override
    public void close() throws IOException {
        try {
            newFileChannel.close();
        } catch (IOException e) {
            buffer.release();
            throw e;
        }
        buffer.release();
    }
}
