package org.hashdb.ms.persistent.hdb;

import org.hashdb.ms.persistent.info.DbInfoBroker;

import java.io.IOException;

/**
 * Date: 2024/3/5 16:48
 *
 * @author Huanyu Mark
 */
public interface DbPersistService extends DbInfoBroker {

    HdbReader openDataReader() throws IOException;

    HdbWriter openDataWriter() throws IOException;
}
