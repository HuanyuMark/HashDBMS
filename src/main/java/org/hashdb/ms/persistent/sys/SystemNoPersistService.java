package org.hashdb.ms.persistent.sys;

import org.hashdb.ms.manager.SystemInfo;

import java.io.IOException;

/**
 * Date: 2024/3/6 1:41
 *
 * @author Huanyu Mark
 */
public class SystemNoPersistService implements SystemPersistService {
    @Override
    public void write(SystemInfo sysInfo) throws IOException {

    }

    @Override
    public SystemInfo read() throws IOException {
        return new SystemInfo();
    }
}
