package org.hashdb.ms.support;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import de.undercouch.bson4jackson.BsonFactory;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.exception.DBSystemException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Date: 2024/2/27 23:30
 *
 * @author Huanyu Mark
 */
public class BsonService {
    public static final ObjectMapper COMMON;

    private static final TypeReference<HashMap<String, HValue<?>>> hValueMapType = new TypeReference<>() {
    };

    static {
        var factory = new BsonFactory();
//        Class<DataType> dataTypeClass = DataType.class;
        COMMON = new ObjectMapper(factory);
    }

    public static void transfer(Object content, File file) throws IOException {
        var out = Files.newOutputStream(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE);
        COMMON.writeValue(out, content);
    }

    public static void transfer(Database db, Path path) throws IOException {
        OutputStream out = Files.newOutputStream(path, StandardOpenOption.WRITE);
        COMMON.writeValue(out, db.values());
    }

    public static HashMap<String, HValue<?>> toHValueMap(Path path) throws IOException {
        InputStream in = Files.newInputStream(path, StandardOpenOption.READ);
        return COMMON.readValue(in, hValueMapType);
    }

//    private final ByteDateValue[] byteDateMap = new ByteDateValue[]{
//
//    };
//
//    private record ByteDateValue(){}

    private record ValueHolder(byte t, Object value) {
    }

    private static class BitSetDeserializer extends StdDeserializer<BitSet> {

        private static final Constructor<BitSet> constructor;

        static {
            try {
                constructor = BitSet.class.getDeclaredConstructor(long[].class);
                constructor.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        private static final TypeReference<long[]> longArrayType = new TypeReference<>() {
        };

        protected BitSetDeserializer() {
            super(BitSet.class);
        }

        private static class MapType extends TypeReference<Map<String, ?>> {
        }

        private static final MapType mapType = new MapType();

        @Override
        public BitSet deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {
            Iterator<long[]> arrays = jp.readValuesAs(longArrayType);
            if (arrays.hasNext()) {
                try {
                    return constructor.newInstance((Object) arrays.next());
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new DBSystemException(e);
                }
            }
            throw new DBSystemException("no array");
        }
    }

    private static class BitSetSerializer extends StdSerializer<BitSet> {
        public BitSetSerializer() {
            super(BitSet.class);
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

        private static final byte[] dt = new byte[]{(byte) DataType.BITMAP.ordinal()};

        @Override
        public void serialize(BitSet value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            var words = getWords(value);
            int wordsInUse = getWordsInUse(value);
            gen.writeStartObject();
            gen.writeBinaryField("dt", dt);
            gen.writeArrayFieldStart("v");
            gen.writeArray(words, 0, wordsInUse);
            gen.writeEndArray();
            gen.writeEndObject();
        }
    }

    public static <T> StdSerializer<T> createSerializer(
            Class<T> target,
            DataType dataType
    ) {
        return new StdSerializer<>(target, true) {
            private final byte[] dt = new byte[]{((byte) dataType.ordinal())};

            @Override
            public void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeStartObject();
                gen.writeBinaryField("dt", dt);
                gen.writeObjectField("v", value);
                gen.writeEndObject();
            }
        };
    }

    private static class HashMapSerializer extends StdSerializer<HashMap<String, ?>> {

        protected HashMapSerializer() {
            super(HashMap.class, true);
        }

        private final byte[] datatype = new byte[]{((byte) DataType.MAP.ordinal())};

        @Override
        public void serialize(HashMap<String, ?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeBinaryField(" t", datatype);
            gen.writeNumberField(" s", value.size());
            for (Map.Entry<String, ?> entry : value.entrySet()) {
                gen.writeObjectField(entry.getKey(), new ValueHolder(((byte) DataType.typeOfRawValue(entry.getValue()).ordinal()), entry.getValue()));
            }
            gen.writeEndObject();
        }
    }

    private static class HashMapDeserializer extends StdDeserializer<HashMap<String, ?>> {

        protected HashMapDeserializer() {
            super(HashMap.class);
        }

        @Override
        public HashMap<String, ?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            // 跳过 JsonToken.START_OBJECT token
            p.nextToken();
            // field name ' t'
            p.getCurrentName();
            p.nextToken();
            // value data type
            p.getBinaryValue();
            p.nextToken();
            // field name ' s'
            p.getCurrentName();
            p.nextToken();
            // value entry size
            int entrySize = p.getIntValue();
            p.nextToken();
            var result = new HashMap<String, Object>(((int) (entrySize / 0.8)));
            for (int i = 0; i < entrySize; i++) {
                p.getCurrentName();
                p.nextToken();
                p.nextToken();
//                p.getCurrentName();
            }
            return null;
        }
    }


    private static class HValueSerializer extends StdSerializer<HValue<?>> {
        protected HValueSerializer(Class<HValue<?>> t) {
            super(HValue.class, true);
        }

        @Override
        public void serialize(HValue<?> hValue, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
//            hValue.dataType()
            gen.writeEndObject();
        }
    }

    private static class HValueDeserializer extends StdDeserializer<HValue<?>> {
        protected HValueDeserializer() {
            super(HValue.class);
        }

        @Override
        public HValue<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            return null;
        }
    }
}
