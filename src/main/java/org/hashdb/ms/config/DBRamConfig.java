package org.hashdb.ms.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.aspect.methodAccess.ConfigLoadOnly;
import org.hashdb.ms.data.OpsTaskPriority;
import org.hashdb.ms.util.Lazy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Date: 2023/11/22 14:21
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
@Getter
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("db.ram")
@EnableAspectJAutoProxy(exposeProxy = true)
public class DBRamConfig {
    public static final Lazy<OpsTaskPriority> DEFAULT_EXPIRED_KEY_DELETE_PRIORITY = Lazy.of(()->{
        DBRamConfig dbRamConfig = HashDBMSApp.ctx().getBean(DBRamConfig.class);
        return dbRamConfig.getExpiredKeyDeletePriority();
    });

    private OpsTaskPriority expiredKeyDeletePriority = OpsTaskPriority.LOW;

    /**
     * 如果为 true, 则数据库里存放的 UNORDERED_MAP 数据类型的java类会变成 {@link java.util.LinkedHashMap}
     * 用以记录json串的字段顺序. 但是, 这样会多耗费一些内存
     */
    private boolean storeLikeJsonSequence = false;

    @ConfigLoadOnly
    public void setExpiredKeyDeletePriority(OpsTaskPriority expiredKeyDeletePriority) {
        log.info("set expiredKeyClearStrategy original:{}", expiredKeyDeletePriority);
        this.expiredKeyDeletePriority = expiredKeyDeletePriority;
    }

    @ConfigLoadOnly
    public void setStoreLikeJsonSequence(boolean storeLikeJsonSequence) {
        this.storeLikeJsonSequence = storeLikeJsonSequence;
    }
}
