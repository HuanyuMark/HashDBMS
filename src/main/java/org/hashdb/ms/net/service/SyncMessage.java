package org.hashdb.ms.net.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.compiler.TransportableCompileResult;
import org.hashdb.ms.net.msg.MessageType;
import org.hashdb.ms.net.msg.ServiceMessage;

/**
 * Date: 2023/12/7 16:23
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SyncMessage extends ServiceMessage {

    private TransportableCompileResult compileResult;

    @Override
    public MessageType getType() {
        return MessageType.SYNC;
    }
}
