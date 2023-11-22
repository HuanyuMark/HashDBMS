package org.hashdb.ms.util;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Date: 2023/11/14 14:05
 * 控制任务是否取消的标注位
 */
public class AbortSignal {
    private final AtomicBoolean abort = new AtomicBoolean(false);
    public boolean isAbort() {
        return abort.compareAndSet(true,true);
    }
    public void abort() {
        abort.compareAndSet(false,true);
    }
}
