package org.hashdb.ms.data;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Date: 2024/1/10 17:13
 *
 * @author Huanyu Mark
 */
public interface DbOpsTask<T> extends OpsTask<T> {
    @Override
    default T get() {
        throw new UnsupportedOperationException();
    }

    T doOps(Database database);

    static <T> DbOpsTask<T> of(Function<Database, T> ops) {
        return new DbOpsTaskImpl<>(ops);
    }

    static <T> DbOpsTask<?> of(Consumer<Database> ops) {
        return new DbOpsTaskImpl<>(db -> {
            ops.accept(db);
            return null;
        });
    }

    @Slf4j
    class DbOpsTaskImpl<T> implements DbOpsTask<T> {
        public static final DbOpsTask<?> EMPTY = new DbOpsTaskImpl<>(db -> null);
        private final CompletableFuture<T> future = new CompletableFuture<>();

        private final Function<Database, T> ops;

        public DbOpsTaskImpl(Function<Database, T> ops) {
            this.ops = ops;
        }

        @Override
        public T doOps(Database database) {
            try {
                T res = ops.apply(database);
                future.complete(res);
                return res;
            } catch (Throwable e) {
                log.error("db ops function throw exception: {}", e.toString());
                future.completeExceptionally(e);
            }
            return null;
        }

        @Override
        public T result() {
            return future.join();
        }

        @Override
        public CompletableFuture<T> future() {
            return future;
        }

        @Override
        public boolean isRead() {
            return false;
        }
    }

    static DbOpsTask<?> empty() {
        return DbOpsTaskImpl.EMPTY;
    }
}
