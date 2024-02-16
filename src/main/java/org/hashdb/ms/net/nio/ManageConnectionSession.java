package org.hashdb.ms.net.nio;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.util.Lazy;
import org.slf4j.Logger;

/**
 * Date: 2024/2/3 21:15
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public class ManageConnectionSession extends ChannelSession {

    private static final Lazy<DBServerConfig> dbServerConfig = Lazy.of(() -> HashDBMSApp.ctx().getBean(DBServerConfig.class));

    private final long id;

    public ManageConnectionSession(long id) {
        this.id = id;
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    protected Logger logger() {
        return log;
    }

}
