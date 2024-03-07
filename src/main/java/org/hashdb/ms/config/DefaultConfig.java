package org.hashdb.ms.config;

import lombok.Getter;
import org.hashdb.ms.support.Checker;
import org.hashdb.ms.support.UserRecord;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Date: 2024/2/27 11:37
 *
 * @author Huanyu Mark
 */
@Getter
@ConfigurationProperties(value = "default", ignoreInvalidFields = true)
public class DefaultConfig {
    private final UserRecord auth;

    private final long recoverCheckInterval;

    private final int connectTimeoutMillis;

    //todo: 支持这个配置, 结合Checker检查. 支持aofConfig,hdbConfig没有这个时, 用这个. dbSystem.close()那里, 调用systemPersistService 持久化
    private final String sysInfoFileName;

    public DefaultConfig(
            UserRecord authentication,
            UserRecord auth,
            Long recoverCheckInterval,
            Integer connectTimeoutMillis,
            String sysInfoFileName) {
        this.auth = Checker.require(() -> new UserRecord("hash", "hash"), authentication, auth);
        this.recoverCheckInterval = Checker.notNegativeOrZero(recoverCheckInterval, 5_000L, STR."illegal value '\{recoverCheckInterval}' of option 'default.recover-check-interval'.");
        this.connectTimeoutMillis = Checker.notNegativeOrZero(connectTimeoutMillis, 5_000, STR."illegal value '\{connectTimeoutMillis}' of option 'default.connect-timeout-millis'.");
        this.sysInfoFileName = Checker.requireSimpleFilename(sysInfoFileName, "sys.info");
    }
}
