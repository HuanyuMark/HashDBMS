package org.hashdb.ms.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hashdb.ms.aspect.methodAccess.DisposableUse;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.HKey;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.function.BiConsumer;

/**
 * Date: 2023/11/22 14:21
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Getter
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("db.ram")
public class DBRamConfig {
    @RequiredArgsConstructor
    public enum ExpiredKeyClearStrategy {
        LOW_PRIORITY(Database::del),
        HIGH_PRIORITY(Database::delHighPriority),
        ;
        private final BiConsumer<Database, HKey> consumer;
        public void invoke(Database database, HKey hKey) {
            consumer.accept(database, hKey);
        }
    }
    private ExpiredKeyClearStrategy expiredKeyClearStrategy;
    @DisposableUse
    public void setExpiredKeyClearStrategy(ExpiredKeyClearStrategy expiredKeyClearStrategy) {
        this.expiredKeyClearStrategy = expiredKeyClearStrategy;
    }
}
