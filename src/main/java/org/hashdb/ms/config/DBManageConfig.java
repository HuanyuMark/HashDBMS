package org.hashdb.ms.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.support.User;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 2024/2/21 18:46
 *
 * @author Huanyu Mark
 * @version 0.0.1
 */
@Slf4j
@Getter
@ConfigurationProperties(value = "db.manage", ignoreInvalidFields = true)
public class DBManageConfig {
    /**
     * 初始用户, 在数据库系统初始化时, 会将这些用户加入到数据库中
     */
    private final List<User> initUsers;

    public DBManageConfig(List<User> initUsers) {
        this.initUsers = initUsers == null ? new ArrayList<>(List.of(new User("hash", "hash"))) : initUsers;
    }
}
