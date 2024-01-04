package org.hashdb.ms.net;

import org.hashdb.ms.data.Database;

/**
 * Date: 2024/1/3 11:11
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface ReadonlyConnectionSession {
    Database getDatabase();
}
