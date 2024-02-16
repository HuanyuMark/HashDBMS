package org.hashdb.ms.persistent;

import lombok.RequiredArgsConstructor;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.config.AofConfig;
import org.hashdb.ms.util.Lazy;

import java.util.LinkedList;
import java.util.List;

/**
 * Date: 2023/12/5 14:37
 *
 * @author huanyuMake-pecdle
 */
@RequiredArgsConstructor
public class AofService {
    private static final Lazy<AofConfig> aofConfig = Lazy.of(() -> HashDBMSApp.ctx().getBean(AofConfig.class));
    private final List<String> commandBuffer = new LinkedList<>();

    public boolean append(String command) {

        return true;
    }

    public void store() {


    }
}