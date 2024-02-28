package org.hashdb.ms.data.result;

import java.util.List;

/**
 * Date: 2023/11/24 15:07
 *
 * @author Huanyu Mark
 */
public record PopPushResult(
        int len,
        List<?> pop
) {
}
