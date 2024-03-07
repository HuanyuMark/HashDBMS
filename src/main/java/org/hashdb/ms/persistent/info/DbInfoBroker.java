package org.hashdb.ms.persistent.info;

import org.hashdb.ms.data.DatabaseInfos;

import java.io.IOException;

/**
 * Date: 2024/3/6 0:30
 *
 * @author Huanyu Mark
 */
public interface DbInfoBroker {
    DatabaseInfos readInfos() throws IOException;

    void writeInfos(DatabaseInfos infos) throws IOException;
}
