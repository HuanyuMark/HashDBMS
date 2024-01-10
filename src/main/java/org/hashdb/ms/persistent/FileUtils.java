package org.hashdb.ms.persistent;

import org.hashdb.ms.exception.DBSystemException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Objects;
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

    protected static Object readObject(File file) {
        Objects.requireNonNull(file);
        try (
                FileInputStream is = new FileInputStream(file);
                ObjectInputStream inputStream = new ObjectInputStream(is);
        ) {
            return inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new DBSystemException(e);
        }
    }

    protected static boolean writeObject(File file, Object object) {
        Objects.requireNonNull(file);
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        ) {
            objectOutputStream.writeObject(object);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
