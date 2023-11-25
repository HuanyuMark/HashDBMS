package org.hashdb.ms.exception;

import lombok.experimental.StandardException;
import org.apache.logging.log4j.util.Strings;

import java.util.Arrays;
import java.util.List;

/**
 * Date: 2023/11/23 11:38
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@StandardException
public class RequiredConfigException extends DBExternalException{
    public static RequiredConfigException of(String key) {
        return of(List.of(key));
    }
    public static RequiredConfigException of(List<String> keys) {
        return new RequiredConfigException("missing required config key: '" + keys +"'");
    }
}
