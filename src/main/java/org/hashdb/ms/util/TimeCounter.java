package org.hashdb.ms.util;

import java.util.Date;

/**
 * Date: 2023/11/19 23:49
 * 计时
 */
public class TimeCounter {
    protected final Date startTime;

    protected TimeCounter(Date startTime) {
        this.startTime = startTime;
    }

    public static TimeCounter start() {
        return new TimeCounter(new Date());
    }

    /**
     * @return 几毫秒
     */
    public long stop() {
        return new Date().getTime() - startTime.getTime();
    }

    public static long costTime(Runnable task) {
        long start = System.currentTimeMillis();
        task.run();
        return System.currentTimeMillis() - start;
    }
}
