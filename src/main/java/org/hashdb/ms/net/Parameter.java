package org.hashdb.ms.net;

import java.util.LinkedList;
import java.util.List;

/**
 * Date: 2024/1/13 21:19
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class Parameter {
    private final Object value;

    final List<String> usedCacheCommands = new LinkedList<>();

    Parameter(Object value) {
        this.value = value;
    }

    public Object value() {
        return value;
    }
}
