package org.hashdb.ms.net.nio;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Date: 2024/2/19 20:31
 *
 * @author huanyuMake-pecdle
 */
public class ServerNodeSet {
    /**
     * {@link  ServerNode#host()}:{@link ServerNode#port()} -> {@link  ServerNode}
     */
    private final Map<String, ServerNode> keyMap = new HashMap<>();

//    private final Map<Integer, ServerNode> idMap = new HashMap<>();


    public ServerNodeSet() {
    }

    public ServerNodeSet(Iterable<ServerNode> nodes) {
        add(nodes);
    }

    public void add(ServerNode node) {
//        node.setDistributionIdChangeCallback(n -> idMap.put(n.getDistributionId(), n));
        var old = keyMap.put(node.key(), node);
        if (old == null || !old.key().equals(node.key())) {
            onChange();
        }
    }

    public void add(ServerNode... nodes) {
        for (ServerNode node : nodes) {
            add(node);
        }
    }

    public void remove(String host, int port) {
        var old = keyMap.remove(STR."\{host}:\{port}");
        if (old != null) {
            onChange();
        }
    }

    public void remove(ServerNode node) {
        var old = keyMap.remove(node.key());
        if (old != null) {
            onChange();
        }
    }

    public ServerNode get(String host, int port) {
        return keyMap.get(STR."\{host}:\{port}");
    }

    public ServerNode get(String key) {
        return keyMap.get(key);
    }

//    public ServerNode get(int distributionId) {
//        return idMap.get(distributionId);
//    }

    public Collection<ServerNode> all() {
        return keyMap.values();
    }

    public void clear() {
        int oldSize = keyMap.size();
        keyMap.clear();
        if (oldSize != 0) {
            onChange();
        }
    }

    public void add(Iterable<ServerNode> nodes) {
        nodes.forEach(this::add);
    }

    protected void onChange() {
    }
}
