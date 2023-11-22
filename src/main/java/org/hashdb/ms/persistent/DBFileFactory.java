package org.hashdb.ms.persistent;

import org.hashdb.ms.exception.DBExternalException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Date: 2023/11/21 14:16
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class DBFileFactory {
    public static final String INDEX_FILE_NAME = "db.index";
    @Contract("_, _ -> new")
    public static @NotNull File newDBChunkFile(File dbFileDir, int chunkId) {
        return new File(dbFileDir,chunkId + ".db");
    }
    public static File newIndexFile(File dbFileDir) {
        return new File(dbFileDir,INDEX_FILE_NAME);
    }
    public static File loadIndexFile(File dbFileDir) {
        File[] files = dbFileDir.listFiles(f -> f.getName().equals(INDEX_FILE_NAME));
        if(files == null || files.length == 0) {
            throw new DBExternalException(new FileNotFoundException("can`t find database index file: '"+
                    Path.of(dbFileDir.getAbsolutePath(),INDEX_FILE_NAME) +"'"));
        }
        return files[0];
    }

    /**
     * @param dbFileDir 数据库文件夹，不是所有数据库文件夹的根目录
     * @return 升序排列的数据块文件
     */
    public static File[] loadDBChunkFile(@NotNull File dbFileDir) {
        File[] shouldBeSort = Objects.requireNonNullElse(dbFileDir.listFiles(f -> f.getName().matches("\\d+\\.db")), new File[0]);
//        Pattern nonNum = Pattern.compile("\\D+");
//        Arrays.parallelSort(shouldBeSort, (f1, f2) -> {
//            String numStr1 = nonNum.matcher(f1.getName()).replaceAll("").trim();
//            String numStr2 = nonNum.matcher(f2.getName()).replaceAll("").trim();
//            return Integer.parseInt(numStr1) - Integer.parseInt(numStr2);
//        });
        return shouldBeSort;
    }
}
