package org.hashdb.ms.net.exception;

import org.hashdb.ms.exception.DBClientException;
import org.hashdb.ms.net.nio.SessionMeta;

/**
 * Date: 2024/2/18 13:01
 *
 * @author huanyuMake-pecdle
 */
public class IllegalUpgradeSessionException extends DBClientException {
    public IllegalUpgradeSessionException(SessionMeta origin, SessionMeta target) {
        super(String.format("Illegal upgrade session from %s to %s", origin.name(), target.name()));
    }
}
