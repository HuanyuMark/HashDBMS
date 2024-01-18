package org.hashdb.ms.net.nio.msg.v1;

/**
 * Date: 2024/1/17 16:35
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class ActMessage<B> extends Message<B> {

    protected final long actId;

    public ActMessage(long id, long actId, B body) {
        super(id, body);
        this.actId = actId;
    }

    public ActMessage(long actId, B body) {
        super(body);
        this.actId = actId;
    }

    public static DefaultActMessage defaultAct(long actId) {
        return new DefaultActMessage(actId, "\"SUCC\"");
    }

    public long actId() {
        return actId;
    }
}
