package org.hashdb.ms.net.service;

import lombok.Getter;
import org.hashdb.ms.net.msg.MessageType;
import org.hashdb.ms.net.msg.ServiceMessage;

import java.net.Socket;

/**
 * Date: 2023/12/1 2:14
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Getter
public class HeartbeatMessage extends ServiceMessage {

    protected String ip;

    protected int port;

    protected long beat;

    public static HeartbeatMessage newBeat(Socket socket) {
        return new HeartbeatMessage(socket);
    }

    public HeartbeatMessage() {
    }

    protected HeartbeatMessage(Socket socket) {
        this(socket.getInetAddress().getHostAddress(), socket.getPort(), 0);
    }

    protected HeartbeatMessage(String ip, int port, long beat) {
        this.ip = ip;
        this.port = port;
        this.beat = beat;
        setTimestamp(System.currentTimeMillis());
    }

    public HeartbeatMessage nextBeat() {
        if (ip == null) {
            throw new NullPointerException("ip is null");
        }
        return new HeartbeatMessage(ip, port, beat + 1);
    }

    @Override
    public MessageType getType() {
        return MessageType.HEARTBEAT;
    }
}
