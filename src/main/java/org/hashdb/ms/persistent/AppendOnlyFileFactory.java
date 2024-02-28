package org.hashdb.ms.persistent;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.AofConfig;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.support.StaticAutowired;
import org.hashdb.ms.util.AsyncService;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date: 2024/1/5 20:32
 *
 * @author Huanyu Mark
 */
@Slf4j
public class AppendOnlyFileFactory {
    @StaticAutowired
    public static AofConfig aofConfig;
    private static final Pattern numberPattern = Pattern.compile("[0-9]+");
    public static final Map<String, AofFile.FileSystemAofFile> databaseAofNewFiles = new HashMap<>();

    protected static AofFile.FileSystemAofFile loadAofNewFiles(String databaseName) {
        var aofFiles = Objects.requireNonNullElse(new File(aofConfig.getRootDir(), databaseName)
                .listFiles(file -> !file.isDirectory() && file.getName().matches("\\d+\\.new\\.aof")), new File[0]);
        return Arrays.stream(aofFiles)
                .max((file1, file2) -> {
                    Matcher matcher1 = numberPattern.matcher(file1.getName());
                    Matcher matcher2 = numberPattern.matcher(file2.getName());
                    matcher1.find();
                    matcher2.find();
                    return Integer.parseInt(matcher1.group()) - Integer.parseInt(matcher2.group());
                })
                .map(content -> AofFile.create(content, aofFiles.length - 1))
                .orElseGet(() -> {
                    File aofFileDir = FileUtils.prepareDir(new File(aofConfig.getRootDir(), databaseName),
                            () -> new DBSystemException(STR."Create aof file directory failed! may be it is existed but it isn`t a directory. root path: '\{aofConfig.getRootDir()}'"));
                    return AofFile.create(new File(aofFileDir, 0 + ".new.aof"), 0);
                });
    }

    public static File newBaseFile(String databaseName) {
        var config = aofConfig;
        var aofFileDir = FileUtils.prepareDir(new File(config.getRootDir(), databaseName),
                () -> new DBSystemException(STR."Create aof file directory failed! may be it is existed but it isn`t a directory. root path: '\{config.getRootDir()}'"));
        return new File(aofFileDir, config.getAofBaseFileName());
    }

    public static AofFile getAofNewFile(String databaseName) {
        var aofNewFile = databaseAofNewFiles.computeIfAbsent(databaseName, AppendOnlyFileFactory::loadAofNewFiles);
        if (aofNewFile.file().length() < aofConfig.getChunkSize()) {
            return aofNewFile;
        }
        AsyncService.start(aofNewFile::flush);
        int newOrder = aofNewFile.order() + 1;
        var emptyAofNewFile = AofFile.create(new File(aofNewFile.file().getParentFile(), STR."\{newOrder}.new.aof"), newOrder);
        databaseAofNewFiles.put(databaseName, emptyAofNewFile);
        return emptyAofNewFile;
    }
}
