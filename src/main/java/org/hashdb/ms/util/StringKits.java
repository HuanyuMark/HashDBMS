package org.hashdb.ms.util;

/**
 * Date: 2024/2/25 20:10
 *
 * @author huanyuMake-pecdle
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
}
