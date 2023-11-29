package org.hashdb.ms.compiler.keyword;

/**
 * Date: 2023/11/24 16:24
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public enum KeywordModifier {
    LIKE,
    SELF
    ;
    public static KeywordModifier of(String key) {
        try {
            return valueOf(key.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
