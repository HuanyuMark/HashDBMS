package org.hashdb.ms.net.msg;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.UUID;

/**
 * Date: 2023/12/1 3:12
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ServiceMessage extends Message {
    {
        setTimestamp(System.currentTimeMillis());
        setId(UUID.randomUUID());
    }
}
