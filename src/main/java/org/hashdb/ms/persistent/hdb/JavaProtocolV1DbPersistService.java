package org.hashdb.ms.persistent.hdb;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.data.DatabaseInfos;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.exception.DBSystemException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * Date: 2024/3/5 17:14
 *
 * @author Huanyu Mark
 */
@Slf4j
public class JavaProtocolV1DbPersistService implements DbPersistService {

    public static final int version = 2;
    private final Hdb hdb;

    JavaProtocolV1DbPersistService(Hdb hdb) {
        this.hdb = hdb;
    }

    @Override
    public DatabaseInfos readInfos() throws IOException {
        try (var in = new ObjectInputStream(Files.newInputStream(hdb.getInfosPath(), StandardOpenOption.READ))) {
            in.skipBytes(1);
            try {
                return ((DatabaseInfos) in.readObject());
            } catch (ClassNotFoundException e) {
                log.error("Unsupported HDB version", e);
                throw new IOException("Unsupported HDB version");
            }
        }
    }

    @Override
    public void writeInfos(DatabaseInfos infos) throws IOException {
        try (var out = new ObjectOutputStream(Files.newOutputStream(hdb.getInfosPath(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
            out.writeByte(version);
            out.writeObject(infos);
        }
    }

    @Override
    public HdbReader openDataReader() throws IOException {
        return new JavaProtocolReader(hdb.getDataPath());
    }

    @Override
    public HdbWriter openDataWriter() throws IOException {
        return new JavaProtocolWriter(hdb.getDataPath());
    }

    public static class JavaProtocolReader implements HdbReader {
        private final Path file;

        JavaProtocolReader(Path file) throws IOException {
            this.file = file;
        }

        @Override
        public void read(Consumer<HValue<?>> dataAcceptor) {
            try (
                    var oin = new ObjectInputStream(Files.newInputStream(file, StandardOpenOption.READ));
            ) {
                oin.skipBytes(1);
                @SuppressWarnings("unchecked")
                Collection<HValue<?>> objects = (Collection<HValue<?>>) oin.readObject();
                objects.forEach(dataAcceptor);
            } catch (IOException | ClassNotFoundException e) {
                throw new DBSystemException(e);
            }
        }

        @Override
        public void close() {
        }
    }

    public static class JavaProtocolWriter implements HdbWriter {

        private final Path file;

        public JavaProtocolWriter(Path file) throws IOException {
            this.file = file;
        }

        @Override
        public void write(Collection<HValue<?>> source) throws IOException {
            try (var objectOutputStream = new ObjectOutputStream(Files.newOutputStream(file, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))) {
                objectOutputStream.writeByte(version);
                objectOutputStream.writeObject(source);
            }
        }

        @Override
        public void close() {
        }
    }
}
