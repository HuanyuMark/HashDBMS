package org.hashdb.ms.persistent.hdb;

/**
 * Date: 2024/3/6 14:26
 *
 * @author Huanyu Mark
 */
public class NopHdb extends AbstractHdb {

    private static NopHdb instance;

    public static NopHdb get() {
        return instance == null ? (instance = new NopHdb()) : instance;
    }

    private NopHdb() {
    }

    @Override
    public void modify() {
    }

    @Override
    public void modify(int delta) {

    }

    @Override
    public void close() throws Exception {
    }
}
