package org.hashdb.ms.data;

import lombok.RequiredArgsConstructor;

/**
 * Date: 2023/11/21 2:37
 * 在{@link Database} 中存储 K - V 的pair
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@RequiredArgsConstructor
public class HValue {
    private final HKey HKey;
    private final Object data;

    public Object data() {
        return data;
    }
    public HKey key() {
        return HKey;
    }
}
