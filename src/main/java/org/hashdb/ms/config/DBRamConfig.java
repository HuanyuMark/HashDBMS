package org.hashdb.ms.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.aspect.methodAccess.ConfigLoadOnly;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.OpsTaskPriority;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.function.BiConsumer;

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
    @RequiredArgsConstructor
    public enum ExpiredKeyClearStrategy {
        LOW_PRIORITY(Database::del),
        HIGH_PRIORITY((db, key) -> {
            db.submitOpsTaskAsync(db.delTask(key), OpsTaskPriority.HIGH);
        }),
        ;
        private final BiConsumer<Database, String> consumer;

        public void invoke(Database database, String key) {
            consumer.accept(database, key);
        }
    }

    private ExpiredKeyClearStrategy expiredKeyClearStrategy = ExpiredKeyClearStrategy.LOW_PRIORITY;

    @ConfigLoadOnly
    public void setExpiredKeyClearStrategy(ExpiredKeyClearStrategy expiredKeyClearStrategy) {
        log.info("set expiredKeyClearStrategy original:{}", expiredKeyClearStrategy);
        this.expiredKeyClearStrategy = expiredKeyClearStrategy;
    }
}
