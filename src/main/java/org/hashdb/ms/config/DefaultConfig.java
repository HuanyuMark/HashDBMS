package org.hashdb.ms.config;

import lombok.Getter;
import org.hashdb.ms.support.Checker;
import org.hashdb.ms.support.User;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Date: 2024/2/27 11:37
 *
 * @author huanyuMake-pecdle
 */
@Getter
@ConfigurationProperties(value = "default", ignoreInvalidFields = true)
public class DefaultConfig {
    private final User auth;

    private final long recoverCheckInterval;

    private final int connectTimeoutMillis;

    public DefaultConfig(User authentication, User auth, Long recoverCheckInterval, Integer connectTimeoutMillis) {
        this.auth = Checker.require(() -> new User("hash", "hash"), authentication, auth);
        this.recoverCheckInterval = Checker.notNegativeOrZero(recoverCheckInterval, 5_000L, STR."illegal value '\{recoverCheckInterval}' of option 'default.recover-check-interval'.");
        this.connectTimeoutMillis = Checker.notNegativeOrZero(connectTimeoutMillis, 5_000, STR."illegal value '\{connectTimeoutMillis}' of option 'default.connect-timeout-millis'.");
    }
}
