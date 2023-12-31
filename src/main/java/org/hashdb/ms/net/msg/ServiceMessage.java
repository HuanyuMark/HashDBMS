package org.hashdb.ms.net.msg;

import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * Date: 2023/12/1 3:12
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@EqualsAndHashCode(callSuper = true)
public abstract class ServiceMessage extends Message {
    {
        setTimestamp(System.currentTimeMillis());
        setId(UUID.randomUUID());
    }
}
