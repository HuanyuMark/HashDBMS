package org.hashdb.ms.persistent.sys;

import org.hashdb.ms.config.AofConfig;
import org.hashdb.ms.config.HdbConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.persistent.aof.AofFlusher;
import org.hashdb.ms.persistent.hdb.AbstractHdb;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Date: 2024/3/5 22:52
 *
 * @author Huanyu Mark
 */
@Configuration
public class SystemPersistServiceProvider {

    @Bean
    @ConditionalOnExpression("#{'${db.aof.path}' != null }")
    public SystemPersistService hSystemProtocolV1AofSystemPersistService(
            AofConfig aofConfig,
            ObjectProvider<AofFlusher> aofProvider,
            ObjectProvider<Database> databaseProvider
    ) {
        return new HSystemProtocolV1AofSystemPersistService(aofConfig, aofProvider, databaseProvider);
    }

    @Bean
    @ConditionalOnExpression("#{'${db.aof.path}' == null && '${db.file.path}' != null }")
    public SystemPersistService hSystemProtocolV1HdbSystemPersistService(
            HdbConfig hdbConfig,
            ObjectProvider<AbstractHdb> hdbProvider) {
        return new HSystemProtocolV1HdbSystemPersistService(hdbConfig, hdbProvider);
    }

    @Bean
    @ConditionalOnExpression("#{'${db.aof.path}' == null && '${db.file.path}' != null }")
    public SystemPersistService systemNoPersistService() {
        return new SystemNoPersistService();
    }
}
