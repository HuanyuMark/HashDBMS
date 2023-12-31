package org.hashdb.ms.net.msg;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Date: 2023/11/20 20:55
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Getter
@Setter
@EqualsAndHashCode
public abstract class Message implements Serializable, Cloneable {
    @Serial
    private static final long serialVersionUID = 31895374964L;

    protected UUID id;

    protected long timestamp;

    protected String data;

    @Override
    public Message clone() {
        try {
            return (Message) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public void setType(MessageType type) {
    }

    abstract public MessageType getType();
}
