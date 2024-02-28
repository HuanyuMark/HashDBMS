package org.hashdb.ms.persistent;

import org.hashdb.ms.support.Exit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * Date: 2024/2/28 14:06
 *
 * @author Huanyu Mark
 */
public enum AofFlushStrategy {
    NO(path -> NoFlusher.get()),
    EACH(path -> {
        try {
            return new EachFlusher(path);
        } catch (IOException e) {
            throw new IOExceptionWrapper(e);
        }
    }),
    EVERY_SECOND(path -> {
        try {
            return new IntervalFlusher(path, 1_000);
        } catch (IOException e) {
            throw new IOExceptionWrapper(e);
        }
    });
    private final Function<Path, AofFileFlusher> flusherFactory;

    AofFlushStrategy(Function<Path, AofFileFlusher> flusherFactory) {
        this.flusherFactory = flusherFactory;
    }

    public AofFileFlusher getFlusher(Path path) {
        try {
            return flusherFactory.apply(path);
        } catch (IOExceptionWrapper wrapper) {
            throw Exit.error("can not open AOF file", wrapper.getCause());
        }
    }

    private static class IOExceptionWrapper extends RuntimeException {
        public IOExceptionWrapper(IOException cause) {
            super(cause);
        }

        @Override
        public synchronized IOException getCause() {
            return ((IOException) super.getCause());
        }
    }
}
