package org.hashdb.ms.net.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.net.msg.MessageType;
import org.hashdb.ms.net.msg.ServiceMessage;

import java.net.Socket;

/**
 * Date: 2023/12/1 2:14
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HeartbeatMessage extends ServiceMessage {

    protected long beat;

    public static HeartbeatMessage newBeat(Socket socket) {
        return new HeartbeatMessage(socket);
    }


    public HeartbeatMessage() {
    }

    protected HeartbeatMessage(Socket socket) {
        beat = 0;
    }


    public HeartbeatMessage next() {
        ++beat;
        return this;
    }

    @Override
    public MessageType getType() {
        return MessageType.HEARTBEAT;
    }
}
