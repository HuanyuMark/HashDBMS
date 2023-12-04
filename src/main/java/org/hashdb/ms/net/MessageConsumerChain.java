package org.hashdb.ms.net;

import org.hashdb.ms.net.msg.Message;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Date: 2023/12/2 17:36
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
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
