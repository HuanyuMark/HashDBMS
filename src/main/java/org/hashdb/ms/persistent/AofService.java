package org.hashdb.ms.persistent;

import lombok.RequiredArgsConstructor;
import org.hashdb.ms.config.AofConfig;
import org.hashdb.ms.support.StaticAutowired;

import java.util.LinkedList;
import java.util.List;

/**
 * Date: 2023/12/5 14:37
 *
 * @author Huanyu Mark
 */
@RequiredArgsConstructor
public class AofService {
    @StaticAutowired
    private static AofConfig aofConfig;
    private final List<String> commandBuffer = new LinkedList<>();

    public boolean append(String command) {

        return true;
    }

    public void store() {


    }
}