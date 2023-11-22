package org.hashdb.ms.communication;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Date: 2023/11/20 20:55
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class Message implements Serializable, Cloneable {
    @Serial
    private static final long serialVersionUID = 0L;
    protected String type;
    protected String command;
    protected Date sendTime;
    protected Object data;
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getCommand() {
        return command;
    }
    public void setCommand(String command) {
        this.command = command;
    }
    public Date getSendTime() {
        return sendTime;
    }
    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }
    public Object getData() {
        return data;
    }
    public void setData(Object data) {
        this.data = data;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Message) obj;
        return Objects.equals(this.type, that.type) &&
                Objects.equals(this.sendTime, that.sendTime) &&
                Objects.equals(this.data, that.data);
    }
    @Override
    public String toString() {
        return "Message[" +
                "type=" + type + ", " +
                "sendTime=" + sendTime + ", " +
                "data=" + data + ']';
    }
}
