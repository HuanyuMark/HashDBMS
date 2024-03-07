package org.hashdb.ms.persistent.hdb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.support.SystemCall;
import org.hashdb.ms.util.AsyncService;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: 2024/3/5 17:42
 *
 * @author Huanyu Mark
 */
@Slf4j
public class AsyncFrequencyHdbPersistBroker implements HdbPersistBroker, AutoCloseable {

    private final Hdb hdb;
    private final DbPersistService persistService;

    @Getter
    private final long interval;

    @Getter
    private final int operationCount;

    private final AtomicInteger modifyCount = new AtomicInteger(0);

    private final ScheduledFuture<?> flushTask;

    public AsyncFrequencyHdbPersistBroker(Hdb hdb, long interval, int operationCount) throws IOException {
        this.hdb = hdb;
        this.persistService = HdbPersistServiceFactory.create(hdb);
        this.interval = interval;
        this.operationCount = operationCount;
        flushTask = AsyncService.setInterval(() -> {
            if (modifyCount.get() < operationCount) {
                return;
            }
            flush();
            modifyCount.set(0);
        }, interval);
    }

    @Override
    public void modify() {
        modifyCount.getAndIncrement();
    }

    @Override
    public void modify(int delta) {
        modifyCount.getAndAdd(delta);
    }

    @Override
    public CompletableFuture<Boolean> flush() {
        return SystemCall.forkRun(() -> {
            try {
                persistService.writeInfos(hdb.getDatabase().getInfos());
                try (var writer = persistService.openDataWriter()) {
                    writer.write(hdb.getDatabase().values());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Database preload(ObjectProvider<Database> databaseProvider) throws IOException {
        var infos = persistService.readInfos();
        return databaseProvider.getObject(infos);
    }

    @Override
    public void transferTo(Database database) throws IOException {
        var data = new HashMap<String, HValue<?>>();
        try (HdbReader reader = persistService.openDataReader()) {
            reader.read(v -> data.put(v.getKey(), v));
        }
        database.unsafeSetData(data);
    }

    @Override
    public void close() throws Exception {
        flushTask.cancel(true);
        if (persistService instanceof AutoCloseable c) {
            c.close();
        }
    }
}
