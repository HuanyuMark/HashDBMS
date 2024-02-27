package org.hashdb.ms.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.data.OpsTaskPriority;
import org.hashdb.ms.util.Lazy;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Date: 2023/11/22 14:21
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
@Getter
@ConfigurationProperties("db.ram")
public class DBRamConfig {
    public static final Lazy<OpsTaskPriority> DEFAULT_EXPIRED_KEY_DELETE_PRIORITY = Lazy.of(() -> {
        var dbRamConfig = HashDBMSApp.ctx().getBean(DBRamConfig.class);
        return dbRamConfig.getExpiredKeyDeletePriority();
    });

    private final OpsTaskPriority expiredKeyDeletePriority;

    /**
     * 如果为 true, 则数据库里存放的 UNORDERED_MAP 数据类型的java类会变成 {@link java.util.LinkedHashMap}
     * 用以记录json串的字段顺序. 但是, 这样会多耗费一些内存
     */
    private final boolean storeLikeJsonSequence;

    public DBRamConfig(OpsTaskPriority expiredKeyDeletePriority, Boolean storeLikeJsonSequence) {
        this.expiredKeyDeletePriority = expiredKeyDeletePriority == null ? OpsTaskPriority.LOW : expiredKeyDeletePriority;
        this.storeLikeJsonSequence = storeLikeJsonSequence != null && storeLikeJsonSequence;
    }
}
