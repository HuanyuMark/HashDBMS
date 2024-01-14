package org.hashdb.ms.util;

import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.util.function.Function;

/**
 * Date: 2023/11/24 20:01
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ReflectCache<T> {
    protected final Class<? extends T> clazz;
    protected final Lazy<Constructor<? extends T>> constructor;

    public final static Function<Class<?>, Constructor<?>> constructorFinder = clazz -> {
        try {
            return clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    };

    @SuppressWarnings("unchecked")
    public ReflectCache(
            Class<? extends T> clazz
    ) {
        this.clazz = clazz;
        this.constructor = Lazy.of(() -> (Constructor<? extends T>) constructorFinder.apply(clazz));
    }

    @SuppressWarnings("unchecked")
    public ReflectCache(
            Class<? extends T> clazz,
            Function<Class<?>, Constructor<?>> constructorFinder
    ) {
        this.clazz = clazz;
        this.constructor = Lazy.of(() -> {
            Constructor<? extends T> cons = (Constructor<? extends T>) constructorFinder.apply(clazz);
            cons.setAccessible(true);
            return cons;
        });
    }

    public Class<? extends T> clazz() {
        return clazz;
    }

    public Constructor<? extends T> constructor() {
        return constructor.get();
    }

    @SneakyThrows
    public T create() {
        return constructor().newInstance();
    }


    public static final ReflectCache<?> NULL = new ReflectCache<>(Object.class) {
        @Override
        public Class<?> clazz() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Constructor<?> constructor() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object create() {
            throw new UnsupportedOperationException();
        }
    };
}
