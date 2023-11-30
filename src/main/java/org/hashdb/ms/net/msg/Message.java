package org.hashdb.ms.net.msg;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Date: 2023/11/20 20:55
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Data
public abstract class Message implements IMessage, Serializable, Cloneable {
    @Serial
    private static final long serialVersionUID = 31895374964L;

    protected long timestamp;

    protected String data;
    @Override
    public Message clone() {
        try {
            return  (Message) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
