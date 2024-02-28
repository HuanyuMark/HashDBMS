package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.DatabaseCompileStream;
import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.util.ReflectCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: 2023/11/24 16:51
 *
 * @author Huanyu Mark
 */
public enum Options {
    EXISTS(ExistsOpCtx.class, List.of("es")),
    LIMIT(LimitOpCtx.class, List.of("lm", "lmt")),
    OLD(OldOpCtx.class, List.of("old")),
    /**
     * 使用默认优先级删除
     */
    EXPIRE(ExpireOpCtx.class, List.of("ep")),
    /**
     * 高优先级删除
     */
    HEXPIRE(HExpireOpCtx.class, List.of("hep")),
    /**
     * 使用低优先级删除
     */
    LEXPIRE(LExpireOpCtx.class, List.of("lep")),
    POP(PopOpCtx.class, List.of("pop")),

    COPY(CopyOpCtx.class, List.of("cp")),

    DESTRUCT(DestructOpCtx.class, List.of("dstc")),
    EXPIRE_STRATEGY("expire-strategy", ExpireStrategyOpCtx.class, List.of("eps")),
    DELETE(DeleteOpCtx.class, List.of("del")),
    HDELETE(HDeleteOpCtx.class, List.of("hdel")),
    LDELETE(LDeleteOpCtx.class, List.of("ldel")),
    ;
    private static Map<String, Options> aliasMap;

    private final OptionCache optionCache;

    public final String fullName;

    Options(Class<? extends OptionCtx<?>> optionClass, List<String> aliases) {
        this.optionCache = new OptionCache(optionClass);
        addAlias(aliases, this);
        fullName = name();
    }

    Options(String fullName, Class<? extends OptionCtx<?>> optionClass, List<String> aliases) {
        this.optionCache = new OptionCache(optionClass);
        addAlias(aliases, this);
        this.fullName = fullName;
    }

    private static void addAlias(@NotNull List<String> aliases, Options option) {
        if (aliasMap == null) {
            aliasMap = new HashMap<>();
        }
        for (String alias : aliases) {
            aliasMap.put(alias.toLowerCase(), option);
        }
    }

    public static Options getOption(@NotNull String optionStr) {
        String normalizedOptionStr = optionStr.toUpperCase();
        if (normalizedOptionStr.length() < 2 || normalizedOptionStr.charAt(0) != '-') {
            return null;
        }
        // '--????'
        int assignPos = normalizedOptionStr.indexOf("=");
        int endIndex = assignPos == -1 ? normalizedOptionStr.length() : assignPos;
        if (normalizedOptionStr.charAt(1) == '-') {
            try {
                return valueOf(normalizedOptionStr.substring(2, endIndex));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        // '-????'
        return aliasMap.get(normalizedOptionStr.substring(1, endIndex));
    }

    @Nullable
    public static OptionCtx<?> compile(@NotNull String unknownToken, DatabaseCompileStream stream) {
        // "?????" 是未知的token
        if (unknownToken.charAt(0) != '-') {
            return null;
        }
        int assignPos = unknownToken.indexOf("=");
        int endIndex = assignPos == -1 ? unknownToken.length() : assignPos;
        // 如果找不到 = 这个配置项本身就可能支持默认值, 可以缺省 value
        var valueStr = assignPos == -1 ? "" : unknownToken.substring(assignPos + 1);
        // "-????" 可能是配置项的长名也可能是短名
        if (unknownToken.charAt(1) == '-') {
            // "--???" 是长名
            var normalizedOptionToken = unknownToken.substring(2, endIndex).toUpperCase();
            try {
                // 这个 token 是 配置项token
                return valueOf(normalizedOptionToken).optionCache.create(valueStr, assignPos, stream);
            } catch (IllegalArgumentException e) {
                throw new CommandCompileException("illegal option: '" + normalizedOptionToken + "'." + stream.errToken(unknownToken));
            }
        }
        // "-[这个字符不是'-'而是其它字符]???" 是短名
        var normalizedOptionToken = unknownToken.substring(1, endIndex).toLowerCase();
        var options = aliasMap.get(normalizedOptionToken);
        if (options == null) {
            throw new CommandCompileException("illegal option alias: '" + normalizedOptionToken + "'." + stream.errToken(unknownToken));
        }
        return options.optionCache.create(valueStr, assignPos, stream);
    }

    public static class OptionCache extends ReflectCache<OptionCtx<?>> {
        private final static Map<String, OptionCtx<?>> flyweightMap = new HashMap<>();

        public OptionCache(Class<? extends OptionCtx<?>> clazz) {
            super(clazz);
        }

        public OptionCtx<?> create(String valueStr, int assignPos, DatabaseCompileStream stream) {
            if (!FlyweightOpCtx.class.isAssignableFrom(clazz)) {
                return create().prepareCompile(valueStr, assignPos, stream);
            }
            return flyweightMap.computeIfAbsent(valueStr, v -> create().prepareCompile(valueStr, assignPos, stream));
        }
    }
}
