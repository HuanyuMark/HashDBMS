package org.hashdb.ms.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Date: 2023/11/27 12:59
 * 这个 HValue 是用来存入硬盘的持久化类
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public record StorableHValue<T>(
        T data,
        Date expireDate,
        OpsTaskPriority deletePriority
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 294388325L;
    static {
        if(log.isTraceEnabled()) {
            log.info("serialVersionUID: {}",serialVersionUID);
        }
    }
}
