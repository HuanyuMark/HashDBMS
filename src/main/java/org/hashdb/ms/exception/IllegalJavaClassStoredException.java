package org.hashdb.ms.exception;

import lombok.experimental.StandardException;

/**
 * Date: 2023/11/22 23:01
 *
 * @author Huanyu Mark
 */
@StandardException
public class IllegalJavaClassStoredException extends DBSystemException {
    public static IllegalJavaClassStoredException of(Class<?> clazz) {
        return new IllegalJavaClassStoredException("can`t store java class '" + clazz + "'");
    }
}
