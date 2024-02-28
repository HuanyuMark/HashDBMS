package org.hashdb.ms.util;

/**
 * Date: 2024/2/25 20:10
 *
 * @author Huanyu Mark
 * @version 0.0.1
 */
public class StringKits {

    /**
     * @param startIndex include
     * @param endIndex   exclude
     */
    public static boolean isBlank(CharSequence sequence, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            if (sequence.charAt(i) != ' ') {
                return false;
            }
        }
        return true;
    }

    public static boolean isEqual(CharSequence str1, int startIndex1, int endIndex1, CharSequence str2, int startIndex2, int endIndex2) {
        if (endIndex1 - startIndex1 != endIndex2 - startIndex2) {
            return false;
        }
        for (int i = startIndex1; i < endIndex1; i++) {
            if (str1.charAt(i) != str2.charAt(2)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEqual(CharSequence str1, CharSequence str2, int startIndex2, int endIndex2) {
        return isEqual(str1, 0, str1.length(), str2, startIndex2, endIndex2);
    }
}
