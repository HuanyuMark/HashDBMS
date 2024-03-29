package org.hashdb.ms.persistent.aof;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.LocalCommandExecutor;
import org.hashdb.ms.config.AofConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.net.nio.BusinessConnectionSession;
import org.hashdb.ms.persistent.info.DbInfoBroker;
import org.hashdb.ms.persistent.info.HashProtocolV1DistDbInfoBroker;
import org.hashdb.ms.support.Exit;
import org.hashdb.ms.support.StaticAutowired;
import org.hashdb.ms.util.AsyncService;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Date: 2024/1/7 0:34
 * 保存方式:
 * 按保存时间间隔分, 有每次
 * <p>
 * {@link AofFlushStrategy#NO}
 * 不刷入硬盘
 * <p>
 * {@link AofFlushStrategy#EACH}
 * 每次追加都会同步地刷入硬盘
 * <p>
 * {@link AofFlushStrategy#EVERY_SECOND}
 * 每过一秒, 都会异步地刷入硬盘
 *
 * @author Huanyu Mark
 */
@Slf4j
public abstract class ReadableAof {

    @Getter
    @StaticAutowired
    private static AofConfig aofConfig;
    protected final DbInfoBroker dbInfoBroker;

    protected final Charset charset = StandardCharsets.UTF_8;
    @Getter
    protected Database database;

    protected ReadableAof(Path dbInfoFilePath, @Nullable Database database) {
        this.dbInfoBroker = new HashProtocolV1DistDbInfoBroker(dbInfoFilePath, charset);
        this.database = database;
    }

    protected ReadableAof(DbInfoBroker dbInfoBroker) {
        this.dbInfoBroker = dbInfoBroker;
    }

    public Database preload(ObjectProvider<Database> dbProvider) throws IOException {
        var infos = dbInfoBroker.readInfos();
        return dbProvider.getObject(infos);
    }

    public void loadData() throws IOException {
        try (var session = new BusinessConnectionSession()) {
            session.setDatabase(database);
            var executor = LocalCommandExecutor.create(session);
            try (LineReader reader = newReader()) {
                executor.compile(reader.readLine()).execute();
            }
        }
    }

    public LineReader readAll() {
        try {
            return newReader();
        } catch (IOException e) {
            throw Exit.error(log, "can not read aof file", e);
        }
    }

    public void readAllAsync(Consumer<String> commandConsumer) {
        AsyncService.run(() -> {
            try (var reader = newReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    commandConsumer.accept(line);
                }
            } catch (IOException e) {
                throw Exit.error(log, "can not read aof file", e);
            }
        });
    }

    protected abstract LineReader newReader() throws IOException;

    static class BufferedReaderAdaptor implements LineReader {

        private final BufferedReader reader;

        public BufferedReaderAdaptor(BufferedReader reader) {
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

        private static NullLineReader INSTANCE;

        static NullLineReader get() {
            if (INSTANCE == null) {
                INSTANCE = new NullLineReader();
            }
            return INSTANCE;
        }

        @Override
        public @Nullable String readLine() {
            return null;
        }

        @Override
        public void close() throws IOException {
        }
    }
}
