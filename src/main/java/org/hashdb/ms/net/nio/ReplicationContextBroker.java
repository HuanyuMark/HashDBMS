package org.hashdb.ms.net.nio;

/**
 * Date: 2024/3/6 21:03
 *
 * @author Huanyu Mark
 */
public abstract class ReplicationContextBroker implements ReplicationContext {

    private ReplicationContext target = NopReplicationContext.get();

    ReplicationContextBroker() {
    }

    public boolean isEnable() {
        return !(target instanceof NopReplicationContext);
    }

    static class NopReplicationContext implements ReplicationContext {
        private static final NopReplicationContext instance = new NopReplicationContext();
        private static final String[] sequence = new String[0];

        public static NopReplicationContext get() {
            return instance;
        }

        private NopReplicationContext() {
        }

        @Override
        public void cache(String command) {
        }

        @Override
        public String[] getIncrSyncSequence(long slaveReplicationOffset) {
            return sequence;
        }

        @Override
        public long getReplicationOffset() {
            return -1;
        }

        @Override
        public void close() {

        }
    }

    public void disableReplication() {
        target = NopReplicationContext.get();
    }

    public void enableReplication(ReplicationContext replicationContext) {
        target = replicationContext;
    }

    @Override
    public void cache(String command) {
        target.cache(command);
    }

    @Override
    public String[] getIncrSyncSequence(long slaveReplicationOffset) {
        return target.getIncrSyncSequence(slaveReplicationOffset);
    }

    @Override
    public long getReplicationOffset() {
        return target.getReplicationOffset();
    }


    protected abstract void close(ReplicationContextBroker broker);

    @Override
    public void close() throws Exception {
    }
}
