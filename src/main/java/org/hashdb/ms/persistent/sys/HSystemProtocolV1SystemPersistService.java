package org.hashdb.ms.persistent.sys;

import io.netty.buffer.ByteBufAllocator;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.DatabaseInfos;
import org.hashdb.ms.manager.SystemInfo;
import org.hashdb.ms.persistent.hdb.HdbPersistServiceFactory;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Objects;

/**
 * Date: 2024/3/6 1:24
 *
 * @author Huanyu Mark
 */
public abstract class HSystemProtocolV1SystemPersistService implements SystemPersistService {

    
    @Override
    public void write(SystemInfo sysInfo) throws IOException {
        var buf = ByteBufAllocator.DEFAULT.buffer();
        var infos = sysInfo.getNavigableDbInfosMap().values();
        buf.writeInt(infos.size());
        for (var info : infos) {
            buf.writeInt(info.getId());
            var nameBytes = info.getName().getBytes(HdbPersistServiceFactory.getCharset());
            buf.writeInt(nameBytes.length);
            buf.writeBytes(nameBytes);
        }
        try (var out = Files.newOutputStream(findSystemInfoFile(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            buf.readBytes(out, buf.readableBytes());
        }
    }

    @Override
    public SystemInfo read() throws IOException {
        Path systemInfoFilepath = findSystemInfoFile();
        var buf = ByteBufAllocator.DEFAULT.buffer();
        try (var in = Files.newInputStream(systemInfoFilepath)) {
            buf.writeBytes(in, in.available());
        } catch (FileNotFoundException e) {
            return new SystemInfo();
        }
        int infoCount = buf.readInt();
        var nameIdMap = new HashMap<String, Integer>((int) (infoCount / 0.8));
        for (int i = 0; i < infoCount; i++) {
            var id = buf.readInt();
            var name = buf.readCharSequence(buf.readInt(), HdbPersistServiceFactory.getCharset()).toString();
            nameIdMap.put(name, id);
        }
        var dbRoot = systemInfoFilepath.getParent().toFile();
        File[] dbDirs = Objects.requireNonNullElseGet(dbRoot.listFiles(File::isDirectory), () -> new File[0]);
        int mapSize = (int) (dbDirs.length / 0.8);
        var nameDbMap = new HashMap<String, Lazy<Database>>(mapSize);
        var idDbMap = new HashMap<Integer, Lazy<Database>>(mapSize);
        var infoDbMap = new HashMap<DatabaseInfos, Lazy<Database>>(mapSize);

        for (File dbDir : dbDirs) {
            Integer id = nameIdMap.get(dbDir.getName());
            if (id == null) {
                continue;
            }
            Lazy<Database> dbLoader;
            dbLoader = createDatabaseLoader(dbDir);
            nameDbMap.put(dbDir.getName(), dbLoader);
            idDbMap.put(id, dbLoader);
            infoDbMap.put(scanDatabaseInfo(dbDir), dbLoader);
        }
        return new SystemInfo(nameDbMap, idDbMap, infoDbMap);
    }

    protected abstract Path findSystemInfoFile() throws IOException;

    protected abstract DatabaseInfos scanDatabaseInfo(File dbDir) throws IOException;

    @NotNull
    protected abstract Lazy<Database> createDatabaseLoader(File dbDir) throws IOException;
}
