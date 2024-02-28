package org.hashdb.ms.net.bio.msg;

import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * Date: 2023/12/1 3:12
 *
 * @author Huanyu Mark
 */
@EqualsAndHashCode(callSuper = true)
public abstract class ServiceMessage extends Message {
    {
        setTimestamp(System.currentTimeMillis());
        setId(UUID.randomUUID());
    }
}
