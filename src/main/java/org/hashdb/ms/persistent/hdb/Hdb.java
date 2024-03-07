package org.hashdb.ms.persistent.hdb;

import lombok.Getter;
import org.hashdb.ms.data.Database;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Date: 2024/2/27 16:59
 * todo: 要完成DatabaseProvider, 以便让Database被IOC管理, 注入依赖
 *
 * @author Huanyu Mark
 */
public class Hdb extends AbstractHdb {

    @Getter
    private final Database database;

    @Getter
    private final Path dataPath;

    @Getter
    private final Path infosPath;

    private final HdbPersistBroker broker;

    private HdbWriter writer;

    public Hdb(Path dataPath, Path infosPath, ObjectProvider<Database> databaseProvider) throws IOException {
        this.dataPath = dataPath;
        this.infosPath = infosPath;
        broker = new AsyncFrequencyHdbPersistBroker(this,
                getHdbConfig().getSaveInterval(),
                getHdbConfig().getOperationCount()
        );
        this.database = broker.preload(databaseProvider);
    }

    public void loadData() throws IOException {
        broker.transferTo(database);
    }

    @Override
    public void close() throws Exception {
        try {
            flush().join();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } finally {
                if (broker instanceof AutoCloseable c) {
                    c.close();
                }
            }
        }
    }

    @Override
    public void modify() {
        broker.modify();
    }

    @Override
    public void modify(int delta) {
        broker.modify(delta);
    }

    public CompletableFuture<Boolean> flush() {
        return broker.flush();
    }
}
