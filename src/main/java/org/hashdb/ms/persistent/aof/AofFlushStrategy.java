package org.hashdb.ms.persistent.aof;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.exception.IOExceptionWrapper;
import org.hashdb.ms.support.Exit;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Date: 2024/2/28 14:06
 *
 * @author Huanyu Mark
 */
@Slf4j
public class AofFlushStrategy {

    public static final AofFlushStrategy NO = new AofFlushStrategy(aof -> {
        try {
            return new SizeAofFlusher(aof, SizeAofFlusher.getAofConfig().getNoFlushStrategyCacheSize());
        } catch (IOException e) {
            throw new IOExceptionWrapper(e);
        }
    });
    public static final AofFlushStrategy EACH = new AofFlushStrategy(path -> {
        try {
            return new EachAofFlusher(path);
        } catch (IOException e) {
            throw new IOExceptionWrapper(e);
        }
    });
    //    public static final AofFlushStrategy EVERY_SECOND_ASYNC = new AofFlushStrategy(file -> {
//        try {
//            return new AsyncIntervalAofFlusher(file, 1_000);
//        } catch (IOException e) {
//            throw new IOExceptionWrapper(e);
//        }
//    });
    public static final AofFlushStrategy EVERY_SECOND = new AofFlushStrategy(file -> {
        try {
            return new SyncIntervalAofFlusher(file, 1_000);
        } catch (IOException e) {
            throw new IOExceptionWrapper(e);
        }
    });

    private record AofFlushStrategyEnum(
            AofFlushStrategy value,
            Field field
    ) {
    }

    private static final AofFlushStrategyEnum[] values = Arrays.stream(AofFlushStrategy.class.getDeclaredFields())
            .filter(f -> Modifier.isFinal(f.getModifiers()) && AofFlushStrategy.class.isAssignableFrom(f.getType()) && !f.getName().equals("values"))
            .map(f -> {
                try {
                    return new AofFlushStrategyEnum(((AofFlushStrategy) f.get(null)), f);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }).toArray(AofFlushStrategyEnum[]::new);

    private final Function<Aof, AofFlusher> flusherFactory;

    protected AofFlushStrategy(Function<Aof, AofFlusher> flusherFactory) {
        this.flusherFactory = flusherFactory;
    }

    public static AofFlushStrategy getIntervalStrategy(long msInterval) {
//        if (async) {
//            if (msInterval == 1_000) {
//                return EVERY_SECOND_ASYNC;
//            }
//            return new AofFlushStrategy(path -> {
//                try {
//                    return new AsyncIntervalAofFlusher(path, msInterval);
//                } catch (IOException e) {
//                    throw new IOExceptionWrapper(e);
//                }
//            });
//        }
        if (msInterval == 1_000) {
            return EVERY_SECOND;
        }
        return new AofFlushStrategy(path -> {
            try {
                return new SyncIntervalAofFlusher(path, msInterval);
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        });
    }

    public static AofFlushStrategy getSizeStrategy(int maxSize) {
        if (maxSize == SizeAofFlusher.getAofConfig().getNoFlushStrategyCacheSize()) {
            return NO;
        }

        return new AofFlushStrategy(path -> {
            try {
                return new SizeAofFlusher(path, maxSize);
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        });
    }

    public static @Nullable AofFlushStrategy matchConstraint(String strategyLike) {
        for (var e : AofFlushStrategy.values) {
            if (e.field().getName().equalsIgnoreCase(strategyLike)) {
                return e.value();
            }
        }
        return null;
    }

    public AofFlusher newFlusher(Aof file) {
        try {
            return flusherFactory.apply(file);
        } catch (IOExceptionWrapper wrapper) {
            throw Exit.error(log, "can not open AOF file", wrapper.getCause());
        }
    }
}
