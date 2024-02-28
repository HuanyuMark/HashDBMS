package org.hashdb.ms.data.projection;

import org.hashdb.ms.support.StaticScanIgnore;

import java.util.Date;

/**
 * Date: 2023/11/21 11:46
 *
 * @author Huanyu Mark
 */
@StaticScanIgnore
public class KeyDetails {
    private final String name;
    private final Date createTime;
    private final Date expireTime;

    public KeyDetails(String name, Date createTime, Date expireTime) {
        this.name = name;
        this.createTime = createTime;
        this.expireTime = expireTime;
    }

    public String getName() {
        return name;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public Date getExpireTime() {
        return expireTime;
    }
}
