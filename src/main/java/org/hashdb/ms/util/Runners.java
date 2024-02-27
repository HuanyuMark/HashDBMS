package org.hashdb.ms.util;

import java.util.Objects;

/**
 * Date: 2023/11/14 15:16
 * 一个运行任务的辅助类
 */
public class Runners {
    @SuppressWarnings("InfiniteLoopStatement")
    public static void everlasting(Runnable runnable) {
        Objects.requireNonNull(runnable);
        while (true) {
            runnable.run();
        }
    }

}
