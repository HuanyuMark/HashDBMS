package org.hashdb.ms.util;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.keyword.CompilerNode;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.keyword.SystemKeyword;
import org.hashdb.ms.config.DBRamConfig;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.exception.IllegalCompilerNodeException;
import org.hashdb.ms.net.bio.msg.Message;
import org.hashdb.ms.net.bio.msg.MessageType;
import org.hashdb.ms.net.exception.IllegalMessageException;
import org.hashdb.ms.net.exception.IllegalMessageTypeException;
import org.hashdb.ms.support.StaticAutowired;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Date: 2023/11/24 23:13
 *
 * @author Huanyu Mark
 */
@Slf4j
public class JsonService {
    public static final ObjectMapper COMMON = new ObjectMapper();
    private static final Version JACKSON_SERIALIZER_VERSION = new Version(1, 0, 1, "dev compiler", "hashdb", "hashDBMS");

    public static String toString(Object obj) {
        try {
            return COMMON.writeValueAsString(obj);
        } catch (IOException e) {
            throw new DBSystemException(e);
        }
    }

    /**
     * @param objs 这些对象对应的json字符串必须形似"{}", 否则将抛出{@link IllegalArgumentException 异常
     */
    public static String mergeObjsToString(Object... objs) throws IllegalArgumentException {
        if (objs.length == 0) {
            return "{}";
        }
        if (objs[0] == null) {
            throw new NullPointerException();
        }
        var r = COMMON.valueToTree(objs[0]);
        if (!(r instanceof ObjectNode rootNode)) {
            throw new IllegalArgumentException("require obj");
        }
        for (int i = 1; i < objs.length; i++) {
            var node = COMMON.valueToTree(objs[i]);
            if (!(node instanceof ObjectNode other)) {
                throw new IllegalArgumentException("require obj");
            }
            rootNode.setAll(other);
        }
        try {
            return COMMON.writeValueAsString(rootNode);
        } catch (JsonProcessingException e) {
            throw new DBSystemException(e);
        }
    }

    public static byte[] toBytes(Object obj) {
        try {
            return COMMON.writeValueAsBytes(obj);
        } catch (IOException e) {
            throw new DBSystemException(e);
        }
    }

    public static void transferTo(Object obj, ByteBuf in) {
        var out = new ByteBufOutputStream(in);
        try {
            COMMON.writeValue(((OutputStream) out), obj);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> @Nullable T parse(String json, Class<T> clazz) throws JsonProcessingException {
        T value = COMMON.readValue(json, clazz);
        return (T) normalizeNumber(value);
    }

    public static @Nullable Object parse(byte[] source) throws IOException {
        return parse(source, Object.class);
    }

    public static <T> @Nullable T parse(byte[] source, Class<T> clazz) throws IOException {
        T value = COMMON.readValue(source, clazz);
        return (T) normalizeNumber(value);
    }

    public static <T> @Nullable T parse(byte[] source, int offset, int length, Class<T> clazz) throws IOException {
        T value = COMMON.readValue(source, offset, length, clazz);
        return (T) normalizeNumber(value);
    }

    public static <T> T parse(InputStream inputStream, Class<T> clazz) throws IOException {
        return (T) normalizeNumber(COMMON.readValue(inputStream, clazz));
    }

    public static <T> T parse(JsonParser jp, Class<T> clazz) throws IOException {
        T value = COMMON.readValue(jp, clazz);
        return (T) normalizeNumber(value);
    }

    private static Object normalizeNumber(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integer) {
            return Long.valueOf(integer);
        }
        if (value instanceof Float floatValue) {
            return Double.valueOf(floatValue);
        }
        return value;
    }

    public static Object parse(String json) throws JsonProcessingException {
        return parse(json, Object.class);
    }

    @StaticAutowired
    private static void config(DBRamConfig dbRamConfig) {
        SimpleModule dataTypeModule = new SimpleModule("hashdb", JACKSON_SERIALIZER_VERSION);
        // jackson 默认使用 LikedHashMap 来存储 Object 型 Json, 其顺序与Json串中规定的顺序一致
        // 如果不需要保持一致,则可以使用这个自定义的反序列化器, 将 Object 型映射的java对象改为 HashMap
        if (!dbRamConfig.isStoreLikeJsonSequence()) {
            dataTypeModule.addDeserializer(Map.class, new HashMapDeserializer());
            // 配合DataType.List的实现类
            dataTypeModule.addDeserializer(List.class, new LinkedListDeserializer());
        }
//        dataTypeModule.addDeserializer(Message.class, new MessageDeserializer());
//        dataTypeModule.addDeserializer(CompilerNode.class, new CompilerNodeDeserializer());
//        COMMON.registerModule(dataTypeModule);
        COMMON.enable(DeserializationFeature.USE_LONG_FOR_INTS);
//        COMMON.enable(DeserializationFeature.)
    }

    private static class HashMapDeserializer extends StdDeserializer<HashMap<Object, Object>> {
        public HashMapDeserializer() {
            super(HashMap.class);
        }

        @Override
        public HashMap<Object, Object> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            JsonNode rootNode = jp.getCodec().readTree(jp);
            HashMap<Object, Object> result = new HashMap<>();
            rootNode.fieldNames().forEachRemaining(filedName -> {
                JsonNode node = rootNode.get(filedName);
                if (node.isTextual()) {
                    result.put(filedName, node.asText());
                    return;
                }
                if (node.isBoolean()) {
                    result.put(filedName, node.asBoolean());
                    return;
                }
                if (node.isFloat() || node.isDouble()) {
                    result.put(filedName, node.asDouble());
                    return;
                }
                if (node.isInt() || node.isIntegralNumber() || node.isLong()) {
                    result.put(filedName, node.asLong());
                    return;
                }
                if (node.isArray()) {
                    try (
                            JsonParser arrParser = node.traverse();
                    ) {
                        arrParser.setCodec(jp.getCodec());
                        result.put(filedName, COMMON.readValue(arrParser, List.class));
                        return;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (node.isObject()) {
                    try (
                            JsonParser objParser = node.traverse();
                    ) {
                        objParser.setCodec(jp.getCodec());
                        result.put(filedName, deserialize(objParser, ctxt));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            return result;
        }
    }

    private static class LinkedListDeserializer extends StdDeserializer<List<Object>> {
        public LinkedListDeserializer() {
            super(List.class);
        }

        @Override
        public List<Object> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            JsonNode rootNode = jp.getCodec().readTree(jp);
            LinkedList<Object> result = new LinkedList<>();
            rootNode.elements().forEachRemaining(node -> {
                if (node.isTextual()) {
                    result.add(node.asText());
                    return;
                }
                if (node.isBoolean()) {
                    result.add(node.asBoolean());
                    return;
                }
                if (node.isFloat() || node.isDouble()) {
                    result.add(node.asDouble());
                    return;
                }
                if (node.isInt() || node.isIntegralNumber() || node.isLong()) {
                    result.add(node.asLong());
                    return;
                }
                if (node.isArray()) {
                    try (
                            JsonParser arrParser = node.traverse();
                    ) {
                        result.add(deserialize(arrParser, ctxt));
                        return;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (node.isObject()) {
                    try (
                            JsonParser objParser = node.traverse();
                    ) {
                        // 这里不关闭会造成资源泄漏吗?
                        objParser.setCodec(jp.getCodec());
                        result.add(objParser.readValueAs(Map.class));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            return result;
        }
    }

    private static class MessageDeserializer extends StdDeserializer<Message> {
        protected MessageDeserializer() {
            super(Message.class);
        }

        @Override
        public Message deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            JsonNode rootNode = jp.getCodec().readTree(jp);
            JsonNode type = rootNode.get("type");
            if (type == null) {
                throw new IllegalMessageException("message type is required");
            }
            MessageType messageType;
            try {
                messageType = MessageType.valueOf(type.textValue());
            } catch (IllegalArgumentException e) {
                throw new IllegalMessageTypeException("illegal message type '" + type + "'");
            }
            return messageType.deserialize(rootNode.traverse(jp.getCodec()), rootNode, ctxt);
        }
    }

    /**
     * 反序列化各个编译结点
     */
    private static class CompilerNodeDeserializer extends StdDeserializer<CompilerNode> {
        protected CompilerNodeDeserializer() {
            super(CompilerNode.class);
        }

        @Override
        public CompilerNode deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {
            JsonNode rootNode = jp.getCodec().readTree(jp);
            JsonNode nameNode = rootNode.get("name");
            if (nameNode == null) {
                throw new IllegalCompilerNodeException("the name of compile node is required");
            }
            String name = nameNode.textValue();
            JsonParser newRootNode = rootNode.traverse(jp.getCodec());
            SupplierKeyword supplierKeyword = SupplierKeyword.typeOfIgnoreCase(name);
            if (supplierKeyword != null) {
                return JsonService.parse(newRootNode, supplierKeyword.constructor().clazz);
            }
            ConsumerKeyword consumerKeyword = ConsumerKeyword.typeOfIgnoreCase(name);
            if (consumerKeyword != null) {
                return JsonService.parse(newRootNode, consumerKeyword.constructor().clazz);
            }
            SystemKeyword systemKeyword = SystemKeyword.typeOfIgnoreCase(name);
            if (systemKeyword != null) {
                return JsonService.parse(newRootNode, systemKeyword.constructor().clazz);
            }
            throw new IllegalCompilerNodeException("illegal compiler node '" + name + "'");
        }
    }

    private static class LazyDeserializer extends StdDeserializer<Lazy<?>> {
        protected LazyDeserializer() {
            super(Lazy.class);
        }

        @Override
        public Lazy<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            Object lazyCacheValue = JsonService.parse(jp, Object.class);
            return Lazy.of(lazyCacheValue);
        }
    }

    private static class LazySerializer extends StdSerializer<Lazy<?>> {
        public LazySerializer() {
            this(null);
        }

        public LazySerializer(Class<Lazy<?>> t) {
            super(t);
        }

        @Override
        public void serialize(Lazy<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            Object result = value.get();
            gen.writeObject(result);
        }
    }
}
