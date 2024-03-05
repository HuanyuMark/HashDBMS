package org.hashdb.ms.persistent.hdb;

import com.sun.nio.file.ExtendedOpenOption;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTaskPriority;
import org.hashdb.ms.support.Exit;
import org.hashdb.ms.util.Lazy;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Consumer;

/**
 * Date: 2024/3/5 16:55
 *
 * @author Huanyu Mark
 */
@Slf4j
public class HdbProtocolV1HdbPersistService implements HdbPersistService {

    private final Path filePath;

    HdbProtocolV1HdbPersistService(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public HdbProtocalV1HdbReader openReader() throws IOException {
        return new HdbProtocalV1HdbReader(filePath);
    }

    @Override
    public HdbProtocalV1HdbHdbWriter openWriter() throws IOException {
        return new HdbProtocalV1HdbHdbWriter(filePath);
    }

    public static class HdbProtocalV1HdbReader implements HdbReader {
        private final FileChannel channel;

        private final long fileSize;
        private final ByteBuf buf;

        private int readSize;

        private final Map<DataType, Lazy<Reader<?>>> serializerMap = new IdentityHashMap<>();

        {
            serializerMap.put(DataType.STRING, Lazy.of(StringReader::new));
            serializerMap.put(DataType.NUMBER, Lazy.of(NumberReader::new));
            serializerMap.put(DataType.BITMAP, Lazy.of(BitMapReader::new));
            serializerMap.put(DataType.LIST, Lazy.of(ListReader::new));
            serializerMap.put(DataType.SET, Lazy.of(UnorderedSetReader::new));
            serializerMap.put(DataType.ORDERED_SET, Lazy.of(OrderedSetReader::new));
            serializerMap.put(DataType.MAP, Lazy.of(UnorderedMapReader::new));
            serializerMap.put(DataType.ORDERED_MAP, Lazy.of(OrderedMapReader::new));
        }

        HdbProtocalV1HdbReader(Path filePath) throws IOException {
            FileChannel channel;
            try {
                channel = FileChannel.open(filePath, StandardOpenOption.READ, ExtendedOpenOption.DIRECT);
            } catch (UnsupportedOperationException e) {
                channel = FileChannel.open(filePath, StandardOpenOption.READ);
            }
            this.channel = channel;
            fileSize = channel.size();
            buf = ByteBufAllocator.DEFAULT.buffer();
            buf.writeBytes(channel, channel.position(), (int) Math.min(1024 * 1024 * 5, fileSize));
        }

        @Override
        public final void read(Consumer<HValue<?>> acceptor) {
            // skip version byte
            buf.skipBytes(1);
            try {
                int mapSize = buf.readInt();
                for (int i = 0; i < mapSize; i++) {
                    ensureBufferSize(4);
                    int keyBytesLength = buf.readInt();
                    readSize += 4;

                    ensureBufferSize(keyBytesLength + 9);
                    var key = buf.readCharSequence(keyBytesLength, HdbPersistServiceProvider.getCharset()).toString();
                    readSize += keyBytesLength;

                    var expireDate = buf.readLong();
                    readSize += 8;

                    var deletePriority = OpsTaskPriority.match(buf.readByte());
                    readSize += 1;

                    ensureBufferSize(1);
                    var data = readObject();
                    acceptor.accept(new HValue<>(key, data, new Date(expireDate), deletePriority));
                }
            } catch (IndexOutOfBoundsException | IOException e) {
                throw Exit.error(log, "HDB reading failed, HDB is broken", "unknown");
            }
        }

        /**
         * 在调用前, 请先调用 {@link #ensureBufferSize(int)} 来确保文件内容以加载到缓冲区
         */
        private Object readObject() throws IOException {
            var dataType = DataType.match(buf.readByte());
            ++readSize;
            return serializerMap.get(dataType).get().read();
        }

        private void ensureBufferSize(int needSize) throws IOException {
            if (buf.readableBytes() < needSize) {
                buf.writeBytes(channel, channel.position(), (int) Math.min(1024 * 1024 * 5, fileSize - readSize));
            }
        }

        private void tryDiscard() {
            if (buf.readerIndex() > 1024 * 1024 * 2) {
                buf.discardReadBytes();
            }
        }

        private interface Reader<V> {
            V read() throws IOException;
        }

        private class StringReader implements Reader<String> {

            @Override
            public String read() throws IOException {
                ensureBufferSize(4);
                int bytesLength = buf.readInt();

                ensureBufferSize(bytesLength);
                return buf.readCharSequence(bytesLength, HdbPersistServiceProvider.getCharset()).toString();
            }
        }

        private class NumberReader implements Reader<Number> {
            @Override
            public Number read() {
                var numberClassKey = buf.readByte();
                var numberClass = DataType.NUMBER.javaClasses()[numberClassKey];
                if (numberClass == Long.class) {
                    return buf.readLong();
                }
                if (numberClass == Double.class) {
                    return buf.readDouble();
                }
                throw new UnsupportedOperationException();
            }
        }

        private abstract class MapReader implements Reader<Map<String, Object>> {
            @Override
            public Map<String, Object> read() throws IOException {
                ensureBufferSize(4);
                int size = buf.readInt();
                readSize += 4;

                Map<String, Object> map;
                try {
                    map = (Map<String, Object>) dataType().reflect().constructor().newInstance(size / 0.8);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                ensureBufferSize(size);
                for (int i = 0; i < size; i++) {
                    ensureBufferSize(4);
                    int keyBytesLength = buf.readInt();
                    readSize += 4;

                    ensureBufferSize(keyBytesLength);
                    String key = buf.readCharSequence(keyBytesLength, HdbPersistServiceProvider.getCharset()).toString();
                    readSize += keyBytesLength;

                    tryDiscard();

                    map.put(key, readObject());
                }
                return map;
            }

            protected abstract DataType dataType();
        }

        private class UnorderedMapReader extends MapReader {
            @Override
            protected DataType dataType() {
                return DataType.ORDERED_MAP;
            }
        }

        private class OrderedMapReader extends MapReader {
            @Override
            protected DataType dataType() {
                return DataType.ORDERED_MAP;
            }
        }

        private abstract class SetReader implements Reader<Set<Object>> {
            @Override
            public Set<Object> read() throws IOException {
                ensureBufferSize(4);
                int size = buf.readInt();
                readSize += 4;

                Set<Object> set;
                try {
                    set = (Set<Object>) dataType().reflect().constructor().newInstance(size / 0.8);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                tryDiscard();
                ensureBufferSize(size);
                for (int i = 0; i < size; i++) {
                    set.add(readObject());
                }
                return set;
            }

            protected abstract DataType dataType();
        }

        private class UnorderedSetReader extends SetReader {
            @Override
            protected DataType dataType() {
                return DataType.SET;
            }
        }

        private class OrderedSetReader extends SetReader {
            @Override
            protected DataType dataType() {
                return DataType.ORDERED_SET;
            }
        }

        private class ListReader implements Reader<List<Object>> {
            @Override
            public List<Object> read() throws IOException {
                ensureBufferSize(4);
                int size = buf.readInt();
                readSize += 4;

                List<Object> list;
                try {
                    list = (List<Object>) DataType.LIST.reflect().constructor().newInstance(size / 0.8);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                tryDiscard();
                ensureBufferSize(size);
                for (int i = 0; i < size; i++) {
                    list.add(readObject());
                }
                return list;
            }
        }

        private class BitMapReader implements Reader<BitSet> {
            private static final Constructor<BitSet> constructor;

            static {
                try {
                    constructor = BitSet.class.getDeclaredConstructor(long[].class);
                    constructor.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public BitSet read() throws IOException {
                ensureBufferSize(4);
                int wordCount = buf.readInt();
                readSize += 4;

                long[] longs = new long[wordCount];
                int longBitSize = wordCount << 3;
                ensureBufferSize(longBitSize);
                for (int i = 0; i < wordCount; i++) {
                    longs[i] = buf.readLong();
                }
                readSize += longBitSize;

                tryDiscard();
                try {
                    return constructor.newInstance((Object) longs);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void close() throws IOException {
            try {
                channel.close();
            } finally {
                buf.release();
            }
        }
    }

    public static class HdbProtocalV1HdbHdbWriter implements HdbWriter {
        private final FileChannel channel;

        private final ByteBuf buf;

        public HdbProtocalV1HdbHdbWriter(Path target) throws IOException {
            buf = ByteBufAllocator.DEFAULT.buffer();
            FileChannel channel;
            try {
                channel = FileChannel.open(target, StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.READ,
                        ExtendedOpenOption.DIRECT);
            } catch (UnsupportedOperationException e) {
                channel = FileChannel.open(target, StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.READ);
            }
            this.channel = channel;
        }

        @Override
        public void write(Collection<HValue<?>> source) throws IOException {
            // [1] version
            buf.writeByte(1);
            // [4] map size
            buf.writeInt(source.size());
            var lock = channel.lock();
            for (HValue<?> hValue : source) {
                byte[] keyBytes = hValue.getKey().getBytes(HdbPersistServiceProvider.getCharset());
                // [4] write key
                buf.writeInt(keyBytes.length);
                // [key length(UTF-8)]
                buf.writeBytes(keyBytes);
                // [8] write expire date
                buf.writeLong(hValue.getExpireDate() == null ? 0 : hValue.getExpireDate().getTime());
                // [1] write delete priority
                buf.writeByte(hValue.getDeletePriority().ordinal());
                writeObject(hValue.data());
                ensureWritable();
                buf.discardReadBytes();
            }
            lock.close();
        }

        private final Map<DataType, Lazy<Writer<?>>> serializerMap = new IdentityHashMap<>();

        {
            serializerMap.put(DataType.NUMBER, Lazy.of(NumberWriter::new));
            serializerMap.put(DataType.STRING, Lazy.of(StringWriter::new));
            serializerMap.put(DataType.LIST, Lazy.of(ListWriter::new));
            serializerMap.put(DataType.MAP, Lazy.of(UnorderedMapWriter::new));
            serializerMap.put(DataType.ORDERED_MAP, Lazy.of(OrderedMapWriter::new));
            serializerMap.put(DataType.SET, Lazy.of(UnorderedSetWriter::new));
            serializerMap.put(DataType.ORDERED_SET, Lazy.of(OrderedSetWriter::new));
            serializerMap.put(DataType.BITMAP, Lazy.of(BitMapWriter::new));
        }

        private void writeObject(Object any) throws IOException {
            @SuppressWarnings("unchecked")
            var serializer = ((Writer<Object>) serializerMap.get(DataType.typeOfRawValue(any)).get());
            serializer.write(any);
        }

        private interface Writer<V> {
            void write(V value) throws IOException;
        }

        private void ensureWritable() throws IOException {
            if (buf.readableBytes() > 1024 * 1024 * 5) {
                channel.position(channel.size());
                buf.readBytes(channel, channel.position(), buf.readableBytes());
                buf.clear();
            }
        }

        @Override
        public void close() throws IOException {
            try {
                if (buf.readableBytes() > 0) {
                    channel.force(true);
                }
                channel.close();
            } finally {
                buf.release();
            }
        }

        private abstract class MapWriter implements Writer<Map<String, ?>> {
            @Override
            public void write(Map<String, ?> value) throws IOException {
                // data type
                buf.writeByte(dataType().ordinal());
                // map size
                buf.writeInt(value.size());
                // serialize all entry
                for (Map.Entry<String, ?> entry : value.entrySet()) {
                    var keyBytes = entry.getKey().getBytes(HdbPersistServiceProvider.getCharset());
                    // key bytes size
                    buf.writeInt(keyBytes.length);
                    // key bytes
                    buf.writeBytes(keyBytes);
                    // check buffer size and flush
                    ensureWritable();
                    // value
                    writeObject(entry.getValue());
                }
            }

            protected abstract DataType dataType();
        }

        private class UnorderedMapWriter extends MapWriter {
            @Override
            protected DataType dataType() {
                return DataType.MAP;
            }
        }

        private class OrderedMapWriter extends MapWriter {
            @Override
            protected DataType dataType() {
                return DataType.ORDERED_MAP;
            }
        }

        private class StringWriter implements Writer<String> {
            @Override
            public void write(String value) throws IOException {
                buf.writeByte(DataType.STRING.ordinal());
                byte[] bytes = value.getBytes(HdbPersistServiceProvider.getCharset());
                buf.writeInt(bytes.length);
                buf.writeBytes(bytes);
            }
        }

        private class NumberWriter implements Writer<Number> {
            @Override
            public void write(Number value) throws IOException {
                var numberClassOrdinary = mapNumberKey(value);
                buf.writeByte(numberClassOrdinary);
                if (value instanceof Long) {
                    buf.writeLong(value.longValue());
                    return;
                }
                if (value instanceof Double) {
                    buf.writeDouble(value.doubleValue());
                    return;
                }
                throw new UnsupportedOperationException();
            }

            private static int mapNumberKey(Number value) {
                var numberClass = value.getClass();
                Class<?>[] numberClasses = DataType.NUMBER.javaClasses();
                for (int i = 0; i < numberClasses.length; i++) {
                    if (numberClasses[i] == numberClass) {
                        return i;
                    }
                }
                throw new UnsupportedOperationException();
            }
        }

        private abstract class SetWriter implements Writer<Set<?>> {
            @Override
            public void write(Set<?> value) throws IOException {
                buf.writeByte(dataType().ordinal());
                buf.writeInt(value.size());
                for (Object o : value) {
                    writeObject(o);
                }
            }

            protected abstract DataType dataType();
        }

        private class UnorderedSetWriter extends SetWriter {
            @Override
            protected DataType dataType() {
                return DataType.SET;
            }
        }

        private class OrderedSetWriter extends SetWriter {
            @Override
            protected DataType dataType() {
                return DataType.ORDERED_SET;
            }
        }

        private class BitMapWriter implements Writer<BitSet> {
            @Override
            public void write(BitSet value) throws IOException {
                buf.writeByte(DataType.BITMAP.ordinal());
                int wordCount = getWordsInUse(value);
                buf.writeInt(wordCount);
                long[] words = getWords(value);
                for (int i = 0; i < wordCount; i++) {
                    buf.writeLong(words[i]);
                }
                ensureWritable();
            }

            private static final Field wordsInUse;

            private static final Field words;

            static {
                try {
                    wordsInUse = BitSet.class.getDeclaredField("wordsInUse");
                    words = BitSet.class.getDeclaredField("words");
                    wordsInUse.setAccessible(true);
                    words.setAccessible(true);
//                BitSet.class.getDeclaredConstructor(long[].class);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }

            private static long[] getWords(BitSet bitSet) {
                try {
                    return (long[]) words.get(bitSet);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            private static int getWordsInUse(BitSet bitSet) {
                try {
                    return (int) wordsInUse.get(bitSet);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private class ListWriter implements Writer<List<?>> {
            @Override
            public void write(List<?> value) throws IOException {
                buf.writeByte(DataType.LIST.ordinal());
                buf.writeInt(value.size());
                for (Object o : value) {
                    writeObject(o);
                }
            }
        }
    }
}
