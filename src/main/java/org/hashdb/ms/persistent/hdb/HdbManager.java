package org.hashdb.ms.persistent.hdb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.DBClientException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.ObjectProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Date: 2023/11/21 14:16
 *
 * @author Huanyu Mark
 */
@Slf4j
@RequiredArgsConstructor
public class HdbManager {

    private final Map<Database, Hdb> HDB_MAP = new HashMap<>();

    private final Map<Path, Hdb> DB_PATH_MAP = new HashMap<>();

    private final ObjectProvider<Database> databaseProvider;

    public Hdb preload(File dbDir) throws IOException {
        return DB_PATH_MAP.computeIfAbsent(dbDir.toPath(), p -> {
            try {
                Hdb hdb = new Hdb(Path.of(dbDir.getPath(), Hdb.getHdbConfig().getHdbName()),
                        Path.of(dbDir.getPath(), Hdb.getHdbConfig().getDbInfoFileName()),
                        databaseProvider
                );
                Hdb old = HDB_MAP.put(hdb.getDatabase(), hdb);
                if (old != null) {
                    old.close();
                }
                return hdb;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Hdb get(Database database) {
        return HDB_MAP.get(database);
    }

    @Deprecated
    public static final String INDEX_FILE_NAME = "index.db";


    @Contract("_, _ -> new")
    @Deprecated
    public static @NotNull File newHDBChunkFile(File dbFileDir, int chunkId) {
        return new File(dbFileDir, chunkId + ".chunk.db");
    }

    @Deprecated
    public static File newIndexFile(File dbFileDir) {
        return new File(dbFileDir, INDEX_FILE_NAME);
    }

    @Deprecated
    public static File loadIndexFile(File dbFileDir) {
        File[] files = dbFileDir.listFiles(f -> f.getName().equals(INDEX_FILE_NAME));
        if (files == null || files.length == 0) {
            throw new DBClientException(new FileNotFoundException("can not find database index file: '" +
                    Path.of(dbFileDir.getAbsolutePath(), INDEX_FILE_NAME) + "'"));
        }
        return files[0];
    }

    /**
     * @param dbFileDir 数据库文件夹，不是所有数据库文件夹的根目录
     * @return 升序排列的数据块文件
     */
    @Deprecated
    public static File[] loadDBChunkFile(@NotNull File dbFileDir) {
        return Objects.requireNonNullElse(dbFileDir.listFiles(f -> f.getName().matches("\\d+\\.chunk\\.db")), new File[0]);
    }
}
