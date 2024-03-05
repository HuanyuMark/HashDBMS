package org.hashdb.ms.data;

/**
 * Date: 2023/11/23 1:46
 *
 * @author Huanyu Mark
 */
public enum OpsTaskPriority {
    LOW,
    HIGH;
    private static final OpsTaskPriority[] values = values();

    public static OpsTaskPriority match(int ordinary) {
        return values[ordinary];
    }
}
