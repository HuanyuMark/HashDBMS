package org.hashdb.ms.exception;

import lombok.Getter;
import lombok.experimental.StandardException;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Date: 2023/11/27 16:22
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@StandardException
public class DatabaseClashException extends DBClientException {
    @Getter
    private final List<String> clashKeys = new LinkedList<>();

    public void clashWith(String... keys) {
        clashKeys.addAll(Arrays.asList(keys));
    }
}
