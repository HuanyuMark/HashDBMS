package org.hashdb.ms.net.bio.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.compiler.TransportableCompileResult;
import org.hashdb.ms.net.bio.msg.MessageType;
import org.hashdb.ms.net.bio.msg.ServiceMessage;

/**
 * Date: 2023/12/7 16:23
 *
 * @author Huanyu Mark
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
