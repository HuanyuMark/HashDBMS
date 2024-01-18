package org.hashdb.ms.compiler.analysis;

import org.jetbrains.annotations.NotNull;

/**
 * Date: 2024/1/15 18:07
 * 表示一个执行过程时间复杂度
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class TimeComplexity implements Comparable<TimeComplexity> {
    private static final int linerUnit = 100;

    private static final int exponentialUnit = 10_000;
    /**
     * O(1)
     */
    private int d;

    /**
     * O(N) O(2N)
     */
    private int linear;

    private int log;

    private int nlog;
    private int exponential;

    private TimeComplexity expression;

    private long idleTime = -1;

    @Override
    public int compareTo(@NotNull TimeComplexity o) {
        return (int) (idleTime() - o.idleTime());
    }

    public long idleTime() {
        if (idleTime > 0) {
            return idleTime;
        }
        idleTime = d + (long) linear * linerUnit
                + (long) (Math.log(log) * 100)
                + (long) (nlog * Math.log(nlog) * 100)
                + (long) exponential * exponentialUnit;
        if (expression != null) {
            idleTime += expression.idleTime();
        }
        return idleTime;
    }

    /**
     * O(1)
     */
    public TimeComplexity d(int v) {
        d = v;
        return this;
    }

    /**
     * O(N) O(2N)
     */
    public TimeComplexity linear(int v) {
        linear = v;
        return this;
    }

    public TimeComplexity log(int v) {
        log = v;
        return this;
    }

    public TimeComplexity exponential(int v) {
        exponential = v;
        return this;
    }

    public TimeComplexity expression(TimeComplexity expression) {
        this.expression = expression;
        return this;
    }
}
