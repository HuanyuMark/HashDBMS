package org.hashdb.ms.support;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;

/**
 * Date: 2024/2/29 17:26
 * <p>
 *
 * @author Huanyu Mark
 */
public class ReplicationCache<E> {

    private final List<E> list = new LinkedList<>();

    private final int capacity;

    public ReplicationCache(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        ArrayDeque<Object> objects = new ArrayDeque<>();
        this.capacity = capacity;
    }

    private void consumeIfNeeded() {
        if (list.size() < capacity) {
            return;
        }
        if (list.isEmpty()) {
            return;
        }
        discard(list.removeFirst());
    }

    public boolean add(E e) {
        consumeIfNeeded();
        return list.add(e);
    }

    private void clear() {
        list.clear();
    }

    protected void discard(E el) {
    }
}
