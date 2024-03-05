package org.hashdb.ms.exception;

import lombok.experimental.StandardException;

import java.util.List;

/**
 * Date: 2023/11/23 11:38
 *
 * @author Huanyu Mark
 */
@StandardException
public class RequiredConfigException extends DBClientException {
    public static RequiredConfigException of(String key) {
        return of(List.of(key));
    }

    public static RequiredConfigException of(List<String> keys) {
        return new RequiredConfigException(STR."missing required config key: '\{keys}'");
    }
}
