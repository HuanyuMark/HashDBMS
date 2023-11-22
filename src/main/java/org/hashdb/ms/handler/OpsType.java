package org.hashdb.ms.handler;

import lombok.Getter;

/**
 * Date: 2023/11/21 1:22
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public enum OpsType {
    SET_VALUE,
    SET_VALUES,
    GET_KEYS,
    GET_VALUES,
    ;
    private OpsConsumer opsConsumer;
    @Getter
    private boolean read;
    public Object consume(OpsEvent<?> event) {
        return opsConsumer.consume(event);
    }
}
