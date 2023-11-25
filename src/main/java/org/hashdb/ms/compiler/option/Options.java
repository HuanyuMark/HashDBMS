package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.TokenCompileStream;
import org.hashdb.ms.exception.CommandCompileException;
import org.hashdb.ms.util.ReflectCacheData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: 2023/11/24 16:51
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public enum Options {
    EXISTS(ExistsOpCtx.class, List.of("es")),
    LIMIT(LimitOpCtx.class, List.of("lm")),
    OLD(OldOpCtx.class, List.of()),
    EXPIRE(ExpireOpCtx.class, List.of("ep","lep")),
    /**
     * 高优先级删除
     */
    HEXPIRE(HExpireOpCtx.class,List.of("hep")),
    POP(PopOpCtx.class, List.of());
    private static Map<String, Options> aliasMap;

    private final OptionCache optionCacheCell;

    Options(Class<? extends OptionCtx<?>> optionClass, List<String> aliases) {
        this.optionCacheCell = new OptionCache(this,optionClass);
        addAlias(aliases, this);
    }

    private static void addAlias(@NotNull List<String> aliases, Options option) {
        if(aliasMap == null) {
            aliasMap = new HashMap<>();
        }
        for (String alias : aliases) {
            aliasMap.put(alias.toLowerCase(), option);
        }
    }

    public static boolean isOption(@NotNull String optionStr) {
        String normalizedOptionStr = optionStr.toUpperCase();
        try {
            valueOf(normalizedOptionStr);
            return true;
        } catch (IllegalArgumentException e) {
            return aliasMap.containsKey(normalizedOptionStr);
        }
    }

    @Nullable
    public static OptionCtx<?> compile(@NotNull String unknownToken, TokenCompileStream stream) {
        // "?????" 是未知的token
        if (unknownToken.charAt(0) != '-') {
            return null;
        }
        int assignPos = unknownToken.indexOf("=");
        // 如果找不到 = 这个配置项本身就可能支持默认值, 可以缺省 value
        var valueStr = assignPos == -1 ? "": unknownToken.substring(assignPos + 1);
        // "-????" 可能是配置项的长名也可能是短名
        if (unknownToken.charAt(1) == '-') {
            // "--???" 是长名
            var normalizedOptionToken =  assignPos == -1 ? unknownToken.substring(2).toUpperCase() : unknownToken.substring(2,assignPos).toUpperCase();
            try {
                // 这个 token 是 配置项token
                return valueOf(normalizedOptionToken).optionCacheCell.create().prepareCompile(valueStr,assignPos, stream);
            } catch (IllegalArgumentException e) {
                throw new CommandCompileException("illegal option: '"+normalizedOptionToken+"'."+stream.errToken(unknownToken));
            }
        }
        // "-[这个字符不是'-'而是其它字符]???" 是短名
        var normalizedOptionToken = assignPos == -1 ? unknownToken.substring(1).toLowerCase() : unknownToken.substring(1,assignPos).toLowerCase();
        var options = aliasMap.get(normalizedOptionToken.toLowerCase());
        if (options == null) {
            throw new CommandCompileException("illegal option alias: '"+normalizedOptionToken+"'."+stream.errToken(unknownToken));
        }
        return options.optionCacheCell.create().prepareCompile(valueStr,assignPos, stream);
    }

    public OptionCache getOptionCacheCell() {
        return optionCacheCell;
    }

    public static class OptionCache extends ReflectCacheData<OptionCtx<?>> {

        private final Options option;

        public OptionCache(Options option, Class<? extends OptionCtx<?>> clazz) {
            super(clazz);
            this.option = option;
        }

        public Options option() {
            return option;
        }
    }
}
