package org.hashdb.ms.net.bio.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.net.bio.msg.MessageType;
import org.hashdb.ms.net.bio.msg.ServiceMessage;

import java.net.Socket;

/**
 * Date: 2023/12/1 2:14
 *
 * @author Huanyu Mark
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
