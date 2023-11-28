package org.hashdb.ms.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.config.DBRamConfig;
import org.hashdb.ms.exception.DBInnerException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Date: 2023/11/24 23:13
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public class JacksonSerializer {
    private static final ObjectMapper COMMON = new ObjectMapper();
    private static final Version JACKSON_SERIALIZER_VERSION = new Version(1, 0, 1, "dev compiler", "hashdb", "hashDBMS");

    private static final OneTimeLazy<?> configCommonObjectMapper = OneTimeLazy.of(()->{
        SimpleModule dataTypeModule = new SimpleModule("dataType", JACKSON_SERIALIZER_VERSION);
        DBRamConfig dbRamConfig = HashDBMSApp.ctx().getBean(DBRamConfig.class);
        if(!dbRamConfig.isStoreLikeJsonSequence()) {
            dataTypeModule.addDeserializer(Map.class, new HashMapDeserializer());
            dataTypeModule.addDeserializer(List.class, new LinkedListDeserializer());
        }
        COMMON.registerModule(dataTypeModule);
        return null;
    });

    public static String stringfy(Object obj) {
        try {
            return COMMON.writeValueAsString(obj);
        } catch (Exception e) {
            throw new DBInnerException(e);
        }
    }

    public static @Nullable Object parse(String json, Class<?> clazz) throws JsonProcessingException {
        configCommonObjectMapper.get();
        Object value = COMMON.readValue(json, clazz);
        if (value == null) {
            return null;
        }
        if (Integer.class.isAssignableFrom(value.getClass())) {
            return Long.valueOf((Integer) value);
        }
        if (Float.class.isAssignableFrom(value.getClass())) {
            return Double.valueOf((Float) value);
        }
        return value;
    }

    public static Object parse(String json) throws JsonProcessingException {
        return parse(json, Object.class);
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
}
