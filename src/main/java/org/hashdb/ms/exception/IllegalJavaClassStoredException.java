package org.hashdb.ms.exception;

import lombok.experimental.StandardException;

/**
 * Date: 2023/11/22 23:01
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@StandardException
public class IllegalJavaClassStoredException extends DBInnerException{
    public static IllegalJavaClassStoredException of(Class<?> clazz) {
        return new IllegalJavaClassStoredException("can`t store java class '"+clazz+"'");
    }
}
