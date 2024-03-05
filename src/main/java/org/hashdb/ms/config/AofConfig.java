package org.hashdb.ms.config;

import com.singularsys.jep.EvaluationException;
import com.singularsys.jep.Jep;
import com.singularsys.jep.ParseException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.persistent.aof.AofFlushStrategy;
import org.hashdb.ms.support.Checker;
import org.hashdb.ms.support.Exit;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;

/**
 * Date: 2023/12/5 16:56
 *
 * @author Huanyu Mark
 */
@Slf4j
@Getter
@ConfigurationProperties(value = "db.aof", ignoreInvalidFields = true)
public class AofConfig extends PersistentConfig {
    /**
     * 是否开启aof持久化
     */
    private final boolean enabled;

    /**
     * 重写相关
     */
    private final RewriteConfig rewrite;

    /**
     * 触发重写时, 会将此时数据库的状态以一系列写命令的
     * 形式, 写入该文件, 在下次aof重写触发前, base.aof不会改变,
     * 只有每次重写时, 才会改变该文件
     */
    protected final String baseFileName = "base.aof";

    /**
     * 最新的追加的命令刷入硬盘里的文件都叫这个
     */
    protected final String newFileName = "new.aof";

    /**
     * 如果该节点是主节点,则该节点的aof持久化策略为slaveFlushStrategy
     */
    protected final AofFlushStrategy flushStrategy;

    /**
     * 如果该节点是从节点,则该节点的aof持久化策略为slaveFlushStrategy,
     * 如果缺省该配置, 则使用 {@link #flushStrategy}
     */
    protected final AofFlushStrategy slaveFlushStrategy;

    /**
     * 规定:当 {@link  #flushStrategy} 或者 {@link  #slaveFlushStrategy}
     * 采用 {@link AofFlushStrategy#NO} 时, aof缓冲区最大大小, 超过后, 即使缓冲区大小
     * 没有满足重写条件, 也会异步刷入硬盘
     */
    protected final int noFlushStrategyCacheSize = 10 * 1024 * 1024;

    public AofConfig(
            String path,
            Boolean enabled,
            RewriteConfig rewire,
            String flushStrategy,
            String slaveFlushStrategy
    ) {
        super(path);
        this.enabled = Checker.require(enabled, false);
        this.rewrite = Checker.require(() -> new RewriteConfig(64 * 1024 * 1024L, null, 100D), rewire);
        this.flushStrategy = Checker.require(resolveStrategy(flushStrategy, "db.aof.flush-strategy"), AofFlushStrategy.EVERY_SECOND);
        this.slaveFlushStrategy = Checker.require(resolveStrategy(slaveFlushStrategy, "db.aof.slave-flush-strategy"), this.flushStrategy, AofFlushStrategy.EVERY_SECOND);
    }

    /**
     * @param strategyLike EVERY_[timeUnit]
     *                     SIZE [byteSize/math expression]
     */
    private static @Nullable AofFlushStrategy resolveStrategy(String strategyLike, String optionName) {
        if (strategyLike == null) {
            return null;
        }
        var constraint = AofFlushStrategy.matchConstraint(strategyLike);
        if (constraint != null) {
            return constraint;
        }
        // 可能是Size策略
        if (strategyLike.toUpperCase().startsWith("SIZE")) {
            return AofFlushStrategy.getSizeStrategy(parseByteSize(strategyLike.substring(4).trim(), optionName));
        }
        var sections = Arrays.stream(strategyLike.split("_")).map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        if (sections.length != 2 || !"EVERY".equalsIgnoreCase(sections[0])) {
            throw Exit.error(log,
                    STR."invalid strategy: \{strategyLike} of option '\{optionName}'",
                    STR."""
                    can not resolve strategyLike string\s\s\{strategyLike}'. \
                    strategy should be(case insensitive): EVERY_[SECOND/MINUTE/HOUR]_[SYNC/ASYNC/(async if omit)], \
                    for example, 'EVERY_SECONE_ASYNC', 'EVERY_MINUTE'\
                    """);
        }
        return AofFlushStrategy.getIntervalStrategy(switch (sections[1].toUpperCase()) {
            case "SECOND" -> 1_000;
            case "MINUTE" -> 60_000;
            case "HOUR" -> 3600_000;
            default ->
                    throw Exit.error(log, STR."invalid strategy: \{strategyLike} of option '\{optionName}'", STR."can not resolve strategyLike string \{strategyLike}'");
        });
    }

    private static int parseByteSize(String byteSizeOrExpression, String optionName) {
        try {
            return Integer.parseInt(byteSizeOrExpression);
        } catch (NumberFormatException ignore) {
        }
        Jep jep = new Jep();
        try {
            jep.parse(byteSizeOrExpression.replaceAll("_", ""));
        } catch (ParseException ex) {
            throw Exit.error(log, STR."invalid strategy SIZE_\{byteSizeOrExpression} of option '\{optionName}'",
                    STR."can not parse \{byteSizeOrExpression}, expect integer of a arithmetic expression");
        }
        try {
            var res = jep.evaluate();
            if (res instanceof Number n) {
                return n.intValue();
            }
            throw new DBSystemException(STR."jep evaluation error. expect Number type but get '\{res}'(type:\{res == null ? "null" : res.getClass()})");
        } catch (EvaluationException ex) {
            throw Exit.error(log, STR."invalid strategy SIZE_\{byteSizeOrExpression} of option '\{optionName}'",
                    STR."can not parse \{byteSizeOrExpression}, expect integer of a arithmetic expression");
        }
    }

    @Getter
    public static class RewriteConfig {
        /**
         * 当base.aof大小超过这个阈值后, 发生重写的条件就变为: 当new.aof 超过 base.aof文件大小*Percentage 时重写
         */
        private final long minSize;
        /**
         * 当new.aof文件的大小超过base.aof(如果base.aof较小, 则使用rewriteThreshold)多少倍时, 触发重写, 生成新的base.aof文件.
         * 再基于最新的base.aof生成最新的new.aof
         */
        private final double percentage;

        protected final String baseFileName = "rewrite.base.aof";

        protected final String newFileName = "rewrite.new.aof";

        public RewriteConfig(
                Long minSize,
                Long threshold,
                Double percentage
        ) {
            this.minSize = Checker.notNegative(Checker.require(minSize, threshold), 1, STR."illegal value '\{minSize}' of option 'db.aof.rewite.min-size'");
            this.percentage = Checker.notNegative(percentage, 1, STR."illegal value '\{percentage}' of option 'db.aof.rewrite.percentage'");
        }
    }
}
