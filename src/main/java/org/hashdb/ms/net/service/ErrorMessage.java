package org.hashdb.ms.net.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hashdb.ms.exception.DBClientException;
import org.hashdb.ms.net.msg.MessageType;
import org.hashdb.ms.net.msg.ServiceMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Date: 2023/12/1 16:19
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ErrorMessage extends ServiceMessage {

    private final String error;

    private final String cause;

    public ErrorMessage(@NotNull DBClientException e) {
        this.error = e.getClass().getSimpleName();
        this.cause = e.getMessage();
    }

    @Override
    public MessageType getType() {
        return MessageType.ERROR;
    }
}
