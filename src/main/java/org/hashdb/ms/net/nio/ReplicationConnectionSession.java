package org.hashdb.ms.net.nio;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.CompileStream;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.net.Parameter;
import org.hashdb.ms.net.nio.msg.v1.CloseMessage;
import org.hashdb.ms.net.nio.protocol.Protocol;
import org.hashdb.ms.support.StaticAutowired;
import org.hashdb.ms.util.CacheMap;
import org.hashdb.ms.util.JsonService;

/**
 * Date: 2024/2/3 21:15
 * 从机连到主机, 主机维护的Session
 *
 * @author Huanyu Mark
 */
@Slf4j
public class ReplicationConnectionSession implements BaseConnectionSession {
    @StaticAutowired
    private static ReplicationGroup group;
    private final BusinessConnectionSession base;
    //    private ServerNode client;
    private ReplicationContext replicationContext;

    public ReplicationConnectionSession(BusinessConnectionSession base) {
        this.base = base;
//        base.getCommandExecuteHandler().checkCommandExecutable(compileResult -> {
//            return group.isMaster() || !compileResult.isWrite();
//        }).orElseExecute(((compileResult, ctx, request) -> {
//
//        }));
//        bindHandlers(base.channel());
    }

//    public ServerNode endpoint() {
//        return client;
//    }

    private void bindHandlers(Channel channel) {
        var pipeline = channel.pipeline();
        var incorporator = UncaughtExceptionLogger.extract(pipeline);


        incorporator.restore();
    }

    @Override
    public void onClose(TransientConnectionSession session) {
        base.onClose(session);
    }

    @Override
    public SessionMeta getMeta() {
        return SessionMeta.MANAGEMENT;
    }

    @Override
    public int id() {
        return base.id();
    }

    @Override
    public Channel channel() {
        return base.channel();
    }

    @Override
    public CacheMap<String, CompileStream<?>> getLocalCommandCache() {
        return base.getLocalCommandCache();
    }

    @Override
    public void setDatabase(Database database) {
        base.setDatabase(database);
        replicationContext = group.getReplicationContext(database);
    }

    @Override
    public Database getDatabase() {
        return base.getDatabase();
    }

    @Override
    public Parameter setParameter(String name, Object value) {
        return base.setParameter(name, value);
    }

    @Override
    public Parameter getParameter(String name) {
        return base.getParameter(name);
    }

    @Override
    public void onChannelChange(Channel channel) {
        base.onChannelChange(channel);
        bindHandlers(channel);
    }

    @Override
    public void protocol(Protocol protocol) {
        base.protocol(protocol);
    }

    @Override
    public Protocol protocol() {
        return base.protocol();
    }

    @Override
    public void startInactive() {
        base.startInactive();
    }

    @Override
    public void stopInactive() {
        base.stopInactive();
    }

    @Override
    public void close(CloseMessage closeMessage) {
        base.close(closeMessage);
    }

    @Override
    public String username() {
        return base.username();
    }

    @Override
    public boolean isActive() {
        return base.isActive();
    }

    @Override
    public void close() {
        base.close();
    }

    @Override
    public String toString() {
        return STR."Session[\{getMeta()}] \{JsonService.mergeObjsToString(base)}";
    }
}
