package org.hashdb.ms.net.nio;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Date: 2024/2/19 20:31
 *
 * @author Huanyu Mark
 */
public class ServerNodeSet<N extends ServerNode> {
    /**
     * {@link  ServerNode#host()}:{@link ServerNode#port()} -> {@link  ServerNode}
     */
    private final Map<String, N> keyMap = new HashMap<>();

//    private final Map<Integer, ServerNode> idMap = new HashMap<>();


    public ServerNodeSet() {
    }

    public ServerNodeSet(Iterable<N> nodes) {
        add(nodes);
    }

    public void add(N node) {
//        node.setDistributionIdChangeCallback(n -> idMap.put(n.getDistributionId(), n));
        var old = keyMap.put(node.key(), node);
        if (old == null || !old.key().equals(node.key())) {
            onChange();
            onAdd(node);
        }
    }

    @SafeVarargs
    public final void add(N... nodes) {
        for (var node : nodes) {
            add(node);
        }
    }

    public void remove(String host, int port) {
        var old = keyMap.remove(STR."\{host}:\{port}");
        if (old != null) {
            onChange();
        }
    }

    public void remove(N node) {
        var old = keyMap.remove(node.key());
        if (old != null) {
            onChange();
        }
    }

    public N get(String host, int port) {
        return keyMap.get(STR."\{host}:\{port}");
    }

    public N get(String key) {
        return keyMap.get(key);
    }

//    public N get(int distributionId) {
//        return idMap.get(distributionId);
//    }

    public Collection<N> all() {
        return keyMap.values();
    }

    public void clear() {
        int oldSize = keyMap.size();
        keyMap.clear();
        if (oldSize != 0) {
            onChange();
        }
    }

    public void add(Iterable<N> nodes) {
        nodes.forEach(this::add);
    }

    protected void onChange() {
    }

    protected void onAdd(ServerNode node) {
    }
}
