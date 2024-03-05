package org.hashdb.ms.persistent.hdb;

import org.hashdb.ms.data.HValue;
import org.hashdb.ms.exception.DBSystemException;

import java.io.*;
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
public class JavaProtocolHdbPersistService implements HdbPersistService {
    private final Path target;

    JavaProtocolHdbPersistService(Path target) {
        this.target = target;
    }

    @Override
    public HdbReader openReader() throws IOException {
        return new JavaProtocolReader(target);
    }

    @Override
    public HdbWriter openWriter() throws IOException {
        return new JavaProtocolWriter(target);
    }

    public static class JavaProtocolReader implements HdbReader {
        private final InputStream in;

        JavaProtocolReader(Path file) throws IOException {
            in = Files.newInputStream(file, StandardOpenOption.READ);
        }

        @Override
        public void read(Consumer<HValue<?>> acceptor) {
            try (
                    var oin = new ObjectInputStream(in);
            ) {
                @SuppressWarnings("unchecked")
                Collection<HValue<?>> objects = (Collection<HValue<?>>) oin.readObject();
                objects.forEach(acceptor);
            } catch (IOException | ClassNotFoundException e) {
                throw new DBSystemException(e);
            }
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    public static class JavaProtocolWriter implements HdbWriter {

        private final OutputStream out;

        public JavaProtocolWriter(Path target) throws IOException {
            this.out = Files.newOutputStream(target, StandardOpenOption.WRITE);
        }

        @Override
        public void write(Collection<HValue<?>> source) throws IOException {
            try (var objectOutputStream = new ObjectOutputStream(out)) {
                objectOutputStream.writeObject(source);
            }
        }

        @Override
        public void close() throws IOException {
            out.close();
        }
    }
}
