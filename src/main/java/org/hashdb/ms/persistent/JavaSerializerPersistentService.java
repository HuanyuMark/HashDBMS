package org.hashdb.ms.persistent;

import org.hashdb.ms.config.DBFileConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.DatabaseInfos;
import org.hashdb.ms.data.HKey;
import org.hashdb.ms.exception.DBExternalException;
import org.hashdb.ms.exception.DBFileAccessFailedException;
import org.hashdb.ms.exception.NotFoundDatabaseException;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jol.info.ClassLayout;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

/**
 * Date: 2023/11/21 3:02
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Component
public class JavaSerializerPersistentService extends FileSystemPersistentService {
    public JavaSerializerPersistentService(DBFileConfig dbFileConfig) {
        super(dbFileConfig);
    }

    /**
     * 缺陷：该操作不具有原子性，抛出异常后， 可能会有文件碎片残留
     * @param database 数据库
     */
    @Override
    public boolean persist(Database database) {
        File dbFileDir = getDBFileDir(database);

        // 将数据库的 HashTable 分块写入多个 {chunkId}.db 文件
        HashMap<HKey, Object> buffer = new HashMap<>();
        int[] chunkId = {0};
        database.forEach(value -> {
            buffer.put(value.key(), value.data());
            long size = ClassLayout.parseInstance(buffer).getLossesTotal();
            if (size >= dbFileConfig.getChunkSize()) {
                File dbChunkFile = DBFileFactory.newDBChunkFile(dbFileDir, chunkId[0]++);
                try (
                        FileOutputStream tableValueFileOutputStream = new FileOutputStream(dbChunkFile);
                        ObjectOutputStream tableValueOutputStream = new ObjectOutputStream(tableValueFileOutputStream);
                ) {
                    tableValueOutputStream.writeObject(buffer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                buffer.clear();
            }
        });

        // 写入数据库索引文件， 保存数据库的基本信息
        File indexFile = DBFileFactory.newIndexFile(dbFileDir);
        FileUtils.prepareDir(indexFile, () -> new DBFileAccessFailedException("can`t access index db file '" + indexFile.getAbsolutePath() + "'"));
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(indexFile);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        ) {
            DatabaseInfos dbInfos = database.getInfos();
            dbInfos.setLastSaveTime(new Date());
            objectOutputStream.writeObject(dbInfos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    /**
     * @return 读取数据库根文件目录下所有数据库
     */
    @Override
    public List<Database> scanDatabases() {
        return Arrays.stream(Objects.requireNonNullElse(dbFileConfig.getDbFileRootDir().listFiles(),new File[0]))
                .parallel()
                .map(JavaSerializerPersistentService::scanDatabase)
                .toList();
    }
    @Override
    public Database scanDatabase(String name) {
        File dbFileDir = new File(dbFileConfig.getDbFileRootDir(), name);
        if(!dbFileDir.exists()) {
            throw NotFoundDatabaseException.of(name);
        }
        if(!dbFileDir.isDirectory()) {
            throw new DBExternalException("db file '" + dbFileDir.getAbsolutePath() + "' is not a directory");
        }
        return scanDatabase(dbFileDir);
    }

    @NotNull
    private static Database scanDatabase(File dbFileDir) {
        // 读取数据库索引文件，获取数据库基本信息
        File indexFile = DBFileFactory.loadIndexFile(dbFileDir);
        DatabaseInfos databaseInfos;
        try (
                FileInputStream is = new FileInputStream(indexFile);
                ObjectInputStream inputStream = new ObjectInputStream(is);
        ){
            databaseInfos = (DatabaseInfos) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        // 读取数据库的 HashTable 分块
        File[] dbChunkFile = DBFileFactory.loadDBChunkFile(dbFileDir);
        HashMap<HKey, Object> allEntries = new HashMap<>();
        Arrays.stream(dbChunkFile).parallel().forEach(file-> {
            try (
                    FileInputStream is = new FileInputStream(file);
                    ObjectInputStream inputStream = new ObjectInputStream(is);
            ){
                @SuppressWarnings("unchecked")
                HashMap<HKey, Object> chunk = (HashMap<HKey, Object>) inputStream.readObject();
                allEntries.putAll(chunk);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        return new Database(databaseInfos, allEntries);
    }

}
