package org.hashdb.ms.handler;

import org.springframework.stereotype.Component;

/**
 * Date: 2023/11/21 1:15
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Component
public class OpsConsumerDispatcher {
    public Object dispatch(OpsEvent<?> event) {
        return null;
    }
}
