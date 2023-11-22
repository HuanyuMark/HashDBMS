package org.hashdb.ms.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * Date: 2023/11/21 12:53
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Getter
@RequiredArgsConstructor
public class DatabaseInfos {
    protected final String name;
    protected final Date createTime;
    @Setter
    protected Date lastSaveTime = new Date();
}
