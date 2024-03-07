package org.hashdb.ms.persistent.aof;

import io.netty.buffer.ByteBuf;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.AofConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.persistent.FileUtils;
import org.hashdb.ms.support.Exit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Date: 2024/1/5 20:32
 *
 * @author Huanyu Mark
 */
@Slf4j
@RequiredArgsConstructor
public class AofManager {
    private final AofConfig aofConfig;
    private final Pattern numberPattern = Pattern.compile("[0-9]+");
    private final Map<Database, Aof> databaseAofMap = new ConcurrentHashMap<>();

    private final Map<Path, Aof> dbDirMap = new ConcurrentHashMap<>();

    /**
     * @param content 一般是远程传过来的aof文件
     */
    public static ReadonlyAof createReadonly(ByteBuf content) {
        return new ReadonlyAof(content);
    }

    private Aof create(Database db) {
        var databaseName = db.getInfos().getName();
        var dbDir = FileUtils.prepareDir(new File(aofConfig.getRootDir(), databaseName), f ->
                new DBSystemException(STR."can not create aof file directory \{f.toPath().toAbsolutePath()}"));
        return doCreate(dbDir.toPath(), db);
    }

    public Aof get(Path dbDir) {
        return dbDirMap.computeIfAbsent(dbDir, this::doCreate);
    }

    private Aof doCreate(Path dbDir) {
        return doCreate(dbDir, null);
    }

    private Aof doCreate(Path dbDir, Database db) {
        try {
            return new Aof(
                    Path.of(dbDir.toString(), aofConfig.getBaseFileName()),
                    Path.of(dbDir.toString(), aofConfig.getBaseFileName()),
                    Path.of(dbDir.toString(), aofConfig.getRewrite().getBaseFileName()),
                    Path.of(dbDir.toString(), aofConfig.getRewrite().getNewFileName()),
                    Path.of(dbDir.toString(), aofConfig.getDbInfoFileName()),
                    db
            );
        } catch (IOException e) {
            throw Exit.error(log, "can not create aof file", e);
        }
    }

    /**
     * @param database 数据库
     * @return aofFile
     */
    public Aof get(Database database) {
        return databaseAofMap.computeIfAbsent(database, this::create);
    }
}
