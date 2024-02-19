package org.hashdb.ms.net.nio;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.CompileStream;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.net.Parameter;
import org.hashdb.ms.net.nio.msg.v1.CloseMessage;
import org.hashdb.ms.net.nio.protocol.Protocol;
import org.hashdb.ms.util.CacheMap;

/**
 * Date: 2024/2/3 21:15
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
public class ManageConnectionSession implements BaseConnectionSession {

    private final BaseConnectionSession base;

    public ManageConnectionSession(BaseConnectionSession base) {
        this.base = base;
    }

    @Override
    public void onClose(TransientConnectionSession session) {
        base.onClose(session);
    }

    @Override
    public SessionMeta getSessionMeta() {
        return SessionMeta.MANAGEMENT;
    }

    @Override
    public long id() {
        return base.id();
    }

    @Override
    public CacheMap<String, CompileStream<?>> getLocalCommandCache() {
        return base.getLocalCommandCache();
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
    }

    @Override
    public void onReleaseChannel() {
        base.onReleaseChannel();
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
    public Channel channel() {
        return base.channel();
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
}
