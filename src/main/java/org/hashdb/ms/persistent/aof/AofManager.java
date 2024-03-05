package org.hashdb.ms.persistent.aof;

import io.netty.buffer.ByteBuf;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.AofConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.persistent.FileUtils;
import org.hashdb.ms.support.Exit;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Date: 2024/1/5 20:32
 *
 * @author Huanyu Mark
 */
@Slf4j
@Component
public class AofManager {
    @Resource
    private AofConfig aofConfig;
    private final Pattern numberPattern = Pattern.compile("[0-9]+");
    private final Map<Database, Aof> databaseAofMap = new ConcurrentHashMap<>();

    /**
     * @param content 一般是远程传过来的aof文件
     */
    public static ReadonlyAof createReadonly(ByteBuf content) {
        return new ReadonlyAof(content);
    }

    private Aof create(Database db) {
        var databaseName = db.getInfos().getName();
        File dbDir = new File(aofConfig.getRootDir(), databaseName);
        FileUtils.prepareDir(dbDir, f ->
                new DBSystemException(STR."can not create aof file directory \{f.toPath().toAbsolutePath()}"));
        try {
            return new Aof(
                    new File(dbDir, aofConfig.getBaseFileName()),
                    new File(dbDir, aofConfig.getBaseFileName()),
                    new File(dbDir, aofConfig.getRewrite().getBaseFileName()),
                    new File(dbDir, aofConfig.getRewrite().getNewFileName()),
                    db);
        } catch (IOException e) {
            throw Exit.error(log, "can not create aof file", e);
        }
    }

    /**
     * @param database 数据库
     * @return aofFile
     */
    public Aof getAof(Database database) {
        return databaseAofMap.computeIfAbsent(database, this::create);
    }
}
