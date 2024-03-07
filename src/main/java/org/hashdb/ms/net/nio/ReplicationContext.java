package org.hashdb.ms.net.nio;

import org.hashdb.ms.support.MasterOnly;

/**
 * Date: 2024/3/6 20:58
 *
 * @author Huanyu Mark
 */
public interface ReplicationContext extends AutoCloseable {
    @MasterOnly
    void cache(String command);

    String[] getIncrSyncSequence(long slaveReplicationOffset);

    long getReplicationOffset();
}
