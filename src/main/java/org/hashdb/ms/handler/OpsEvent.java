package org.hashdb.ms.handler;

import org.hashdb.ms.data.Database;

/**
 * Date: 2023/11/21 1:18
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface OpsEvent<D> {
    OpsType getType();
    Database getDatabase();
    D getData();
}
