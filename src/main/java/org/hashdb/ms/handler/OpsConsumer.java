package org.hashdb.ms.handler;

/**
 * Date: 2023/11/21 1:15
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface OpsConsumer<I,O> {
    O consume(OpsEvent<I> event);
}
