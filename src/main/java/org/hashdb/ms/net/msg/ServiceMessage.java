package org.hashdb.ms.net.msg;

/**
 * Date: 2023/12/1 3:12
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class ServiceMessage extends Message {
    {
        timestamp = System.currentTimeMillis();
    }
}
