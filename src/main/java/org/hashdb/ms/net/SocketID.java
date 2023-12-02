package org.hashdb.ms.net;

import lombok.Getter;
import org.hashdb.ms.net.service.HeartbeatMessage;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;

import java.net.Socket;
import java.nio.channels.SocketChannel;

/**
 * Date: 2023/12/1 12:38
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Getter
public class SocketID {
    private Socket socket;

    private final Lazy<String> id = Lazy.of(() -> socket.getInetAddress().getHostAddress() + ":" + socket.getPort());

    public SocketID(@NotNull Socket socket) {
        this.socket = socket;
    }

    public SocketID(SocketChannel channel) {
        this(channel.socket());
    }

    public HeartbeatMessage beat() {
        return HeartbeatMessage.newBeat(socket);
    }

    public String getIp(){
        return socket.getInetAddress().getHostAddress();
    }

    public int getPort(){
        return socket.getPort();
    }

    public String getId(){
        return id.get();
    }
}
