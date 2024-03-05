package org.hashdb.ms.exception;

/**
 * Date: 2024/3/3 15:48
 *
 * @author Huanyu Mark
 */
public interface ExceptionWrapper<E extends Exception> {
    E unwrap();

    static <T extends Exception> ExceptionWrapper<T> wrap(T e) {
        return () -> e;
    }
}
