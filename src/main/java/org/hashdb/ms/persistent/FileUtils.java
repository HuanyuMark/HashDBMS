package org.hashdb.ms.persistent;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Date: 2023/11/21 15:11
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class FileUtils {
    public static @NotNull File prepareDir(String absPath, Function<File, RuntimeException> supplier) {
        File file = new File(absPath);
        return prepareDir(file, supplier);
    }

    public static @NotNull File prepareDir(String absPath, Supplier<RuntimeException> supplier) {
        File file = new File(absPath);
        return prepareDir(file, supplier);
    }

    @Contract("_, _ -> param1")
    public static @NotNull File prepareDir(@NotNull File file, Function<File, RuntimeException> supplier) {
        if (!file.exists() && !file.mkdir()) {
            throw supplier.apply(file);
        }
        return file;
    }

    @Contract("_, _ -> param1")
    public static @NotNull File prepareDir(@NotNull File file, Supplier<RuntimeException> supplier) {
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw supplier.get();
            }
        } else if (!file.isDirectory()) {
            throw supplier.get();
        }
        return file;
    }
}
