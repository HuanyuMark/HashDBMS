package org.hashdb.ms.data.task;

import org.hashdb.ms.data.OpsTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Date: 2023/11/23 23:10
 *
 * @author huanyuMake-pecdle
 */
public class RefDataTypeOpsTaskSupplier {
    @Contract(value = "_ -> new", pure = true)
    public static @NotNull OpsTask<Integer> len(@NotNull Collection<?> list) {
        return OpsTask.of(list::size);
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull OpsTask<Integer> size(@NotNull Collection<?> list) {
        return OpsTask.of(list::size);
    }
}
