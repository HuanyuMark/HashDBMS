package org.hashdb.ms.persistent.hdb;

import java.io.IOException;

/**
 * Date: 2024/3/5 16:48
 *
 * @author Huanyu Mark
 */
public interface HdbPersistService {
    HdbReader openReader() throws IOException;

    HdbWriter openWriter() throws IOException;
}
