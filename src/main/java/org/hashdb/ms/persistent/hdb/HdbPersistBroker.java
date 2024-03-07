package org.hashdb.ms.persistent.hdb;

import org.hashdb.ms.data.Database;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Date: 2024/3/6 0:17
 *
 * @author Huanyu Mark
 */
public interface HdbPersistBroker {
    void modify();

    void modify(int delta);

    CompletableFuture<Boolean> flush();

    Database preload(ObjectProvider<Database> databaseProvider) throws IOException;

    void transferTo(Database database) throws IOException;
}
