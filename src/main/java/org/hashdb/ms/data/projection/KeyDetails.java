package org.hashdb.ms.data.projection;

import java.util.Date;

/**
 * Date: 2023/11/21 11:46
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
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
