package org.hashdb.ms.net.bio;

import org.hashdb.ms.net.bio.msg.Message;
import org.hashdb.ms.support.StaticScanIgnore;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Date: 2023/12/2 17:36
 *
 * @author Huanyu Mark
 */
@Deprecated
@StaticScanIgnore
public class MessageConsumerChain {

    private final List<MessageConsumer> consumers = new LinkedList<>();
    private ListIterator<MessageConsumer> iterator;

    protected Message target;

    public Object consume(Message msg) throws Exception {
        target = msg;
        iterator = consumers.listIterator();
        return next();
    }

    public Object next() {
        if (iterator.hasNext()) {
            return iterator.next().apply(target, this);
        }
        return null;
    }

    public void removeCurrent() {
        iterator.remove();
    }

    void add(MessageConsumer consumer) {
        consumers.add(consumer);
    }
}
