package org.hashdb.ms.net.nio.msg.v1;

/**
 * Date: 2024/1/17 16:35
 *
 * @author Huanyu Mark
 */
public abstract class ActMessage<B> extends Message<B> {

    protected final int actId;

    public ActMessage(int id, int actId, B body) {
        super(id, body);
        this.actId = actId;
    }

    public ActMessage(int id, Message<?> request, B body) {
        super(id, body);
        this.actId = request.id;
    }

    public ActMessage(int actId, B body) {
        super(body);
        this.actId = actId;
    }

    public ActMessage(Message<?> request, B body) {
        super(body);
        this.actId = request.id;
    }

    public static ActMessage<String> act(int actId) {
        return new DefaultActMessage(actId, "SUCC");
    }

    public int actId() {
        return actId;
    }
}
