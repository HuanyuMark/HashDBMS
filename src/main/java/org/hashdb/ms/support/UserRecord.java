package org.hashdb.ms.support;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * Date: 2024/2/27 11:39
 *
 * @author Huanyu Mark
 */
@ToString
@EqualsAndHashCode
public final class UserRecord {
    private final String username;
    private final String password;

    @ConstructorBinding
    public UserRecord(String username, String password, String uname, String pwd) {
        this.username = Checker.require("123123", "username/uname", username, uname);
        this.password = Checker.require("123123", "password", password, pwd);
    }

    public UserRecord(String username, String password) {
        this(username, password, null, null);
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }
}
