package org.hashdb.ms.util;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.hashdb.ms.net.nio.ServerNodeSet;
import org.hashdb.ms.support.DefaultYamlCommentGenerator;
import org.hashdb.ms.support.Exit;
import org.hashdb.ms.support.YamlComment;
import org.hashdb.ms.support.YamlCommentGenerator;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.serializer.SerializerException;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Date: 2024/2/23 1:23
 *
 * @author Huanyu Mark
 * @version 0.0.1
 */
public class YamlService {

    public static final YAMLMapper ORIGINAL = new YAMLMapper();

    public static final YAMLMapper COMMON = new YAMLMapper();

    static {
        // 不写入缺省的文档开头 ---
        COMMON.getFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.USE_PLATFORM_LINE_BREAKS)
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(JsonParser.Feature.ALLOW_YAML_COMMENTS);
//        var factory = COMMON.getSerializerFactory();
//        if (!(factory instanceof BeanSerializerFactory)) {
//            throw new RuntimeException(STR."default serializer factory is not \{BeanSerializerFactory.class}");
//        }
        var yamlModule = new SimpleModule("hashdb-yaml");
        yamlModule.addSerializer(ServerNodeSet.class, new ServerNodeSetSerializer());
        COMMON.registerModule(yamlModule);
    }

    public static @NotNull ByteArrayOutputStream toByteArrayOutputStream(Charset charset, Object content) throws IOException {
        var stream = new ByteArrayOutputStream();
        COMMON.writeValue(new BufferedWriter(new OutputStreamWriter(stream, charset)), content);
        return stream;
    }

    public static StringWriter toStringWriter(Object content) throws IOException {
        var writer = new StringWriter();
        COMMON.writeValue(writer, content);
        return writer;
    }

    public static String toString(Object content) throws JsonProcessingException {
        return COMMON.writeValueAsString(content);
    }

    public static <T> T parse(File file, Class<T> type) throws IOException {
        return COMMON.readValue(new FileReader(file, StandardCharsets.UTF_8), type);
    }

    private static class ServerNodeSetSerializer extends StdSerializer<ServerNodeSet> {
        public ServerNodeSetSerializer() {
            super(ServerNodeSet.class);
        }

        @Override
        public void serialize(ServerNodeSet value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeObject(value.all());
        }

        public ServerNodeSetSerializer(Class<ServerNodeSet> t) {
            super(t);
        }

        public ServerNodeSetSerializer(JavaType type) {
            super(type);
        }

        public ServerNodeSetSerializer(Class<?> t, boolean dummy) {
            super(t, dummy);
        }

        public ServerNodeSetSerializer(StdSerializer<?> src) {
            super(src);
        }
    }

    private static class CommentSerializer extends StdSerializer<Object> {

        protected static final Method removeIgnorableTypes;

        protected static final Method filterUnwantedJDKProperties;

        protected static final Method filterBeanProperties;

        protected static final Method usesStaticTyping;

        protected static final Method _constructWriter;
        protected static Map<BeanDescription, List<BeanPropertyWriter>> writerCache = new HashMap<>();

        static {
            try {
                removeIgnorableTypes = BeanSerializerFactory.class.getDeclaredMethod("removeIgnorableTypes", SerializationConfig.class, BeanDescription.class, List.class);
                filterUnwantedJDKProperties = BeanSerializerFactory.class.getDeclaredMethod("filterUnwantedJDKProperties", SerializationConfig.class, BeanDescription.class, List.class);
                filterBeanProperties = BeanSerializerFactory.class.getDeclaredMethod("filterBeanProperties", SerializationConfig.class, BeanDescription.class, List.class);
                usesStaticTyping = BasicSerializerFactory.class.getDeclaredMethod("usesStaticTyping", SerializationConfig.class, BeanDescription.class, TypeSerializer.class);
                _constructWriter = BeanSerializerFactory.class.getDeclaredMethod("_constructWriter", SerializerProvider.class, BeanPropertyDefinition.class, PropertyBuilder.class, boolean.class, AnnotatedMember.class);
                removeIgnorableTypes.setAccessible(true);
                filterUnwantedJDKProperties.setAccessible(true);
                filterBeanProperties.setAccessible(true);
                usesStaticTyping.setAccessible(true);
                _constructWriter.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        private final Map<YamlComment, Object> singletonMap = new IdentityHashMap<>();

        public CommentSerializer() {
            this(null);
        }

        public CommentSerializer(Class<Object> t) {
            super(t);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            var valueClass = value.getClass();
            var defaultSerializer = ORIGINAL.getSerializerFactory().createSerializer(provider, provider.constructType(valueClass));
            System.out.println(STR."defaultSerializer hashCode: \{Objects.hashCode(defaultSerializer)}");
//            var defaultSerializer = ORIGINAL.getSerializerProvider().findValueSerializer(valueClass);
            if (!(defaultSerializer instanceof BeanSerializer)) {
                ORIGINAL.writeValue(gen, provider);
//                defaultSerializer.serialize(value, gen, provider);
                return;
            }
            System.out.println("bean serialize");
            var serializationConfig = provider.getConfig();
            var intr = serializationConfig.getAnnotationIntrospector();
            var origType = serializationConfig.constructType(valueClass);
            var beanDesc = serializationConfig.introspect(origType);
            JavaType type;

            if (intr == null) {
                type = serializationConfig.constructType(valueClass);
            } else {
                try {
                    type = intr.refineSerializationType(serializationConfig, beanDesc.getClassInfo(), origType);
                } catch (JsonMappingException e) {
                    provider.reportBadTypeDefinition(beanDesc, e.getMessage());
                    return;
                }
            }
            if (type != origType) { // changes; assume static typing; plus, need to re-introspect if class differs
                if (!type.hasRawClass(origType.getRawClass())) {
                    beanDesc = serializationConfig.introspect(type);
                }
            }

            var result = new CommentCombineMap();
            int lineCount = 1;

            var propertyWriters = findOrAddWriter(beanDesc, provider);
            for (BeanPropertyWriter property : propertyWriters) {
                Object propertyValue;
                try {
                    propertyValue = property.get(value);
                } catch (Exception e) {
                    throw new IOException(e);
                }
                if (propertyValue == null) {
                    result.put(property.getSerializedName(), null);
                    continue;
                }
                var yamlComment = property.getAnnotation(YamlComment.class);
                if (yamlComment == null) {
                    result.put(property.getSerializedName(), propertyValue);
                    continue;
                }
                YamlCommentGenerator generator;
                if (yamlComment.singleton() || yamlComment.generator() == DefaultYamlCommentGenerator.class) {
                    generator = (YamlCommentGenerator) singletonMap.computeIfAbsent(yamlComment, config -> createGenerator(getGeneratorConstructor(config.generator()), property));
                } else {
                    var constructor = singletonMap.computeIfAbsent(yamlComment, config -> getGeneratorConstructor(config.generator()));
                    generator = createGenerator((Constructor<? extends YamlCommentGenerator>) constructor, property);
                }
                var commentLines = generator.generate(value, property.getName(), propertyValue, yamlComment.value());
                if (commentLines == null || commentLines.isEmpty()) {
                    result.put(property.getSerializedName(), propertyValue);
                    continue;
                }
                result.put(property.getSerializedName(), propertyValue, commentLines);
            }
            gen.writeObject(result);
        }

        private static class JsonMappingExceptionWrapper extends RuntimeException {
            public JsonMappingExceptionWrapper(JsonMappingException cause) {
                super(cause);
            }

            @Override
            public synchronized JsonMappingException getCause() {
                return (JsonMappingException) super.getCause();
            }
        }

        protected List<BeanPropertyWriter> findOrAddWriter(BeanDescription beanDesc, SerializerProvider prov) throws JsonMappingException {
//            try {
//                return writerCache.computeIfAbsent(beanDesc, desc -> {
//                    try {
//                        return findProperties(desc, prov);
//                    } catch (JsonMappingException e) {
//                        throw new JsonMappingExceptionWrapper(e);
//                    }
//                });
//            } catch (JsonMappingExceptionWrapper wrapper) {
//                throw wrapper.getCause();
//            }
            return findProperties(beanDesc, prov);
        }

        protected List<BeanPropertyWriter> findProperties(BeanDescription beanDesc, SerializerProvider prov) throws JsonMappingException {
            List<BeanPropertyWriter> props = findBeanProperties(prov, beanDesc);
            if (props == null) {
                props = new ArrayList<>();
            } else {
                removeOverlappingTypeIds(prov, beanDesc, props);
            }
            var serializationConfig = prov.getConfig();
            // [databind#638]: Allow injection of "virtual" properties:
            prov.getAnnotationIntrospector().findAndAddVirtualProperties(serializationConfig, beanDesc.getClassInfo(), props);
            var _factoryConfig = ((BeanSerializerFactory) COMMON.getSerializerFactory()).getFactoryConfig();
            // [JACKSON-440] Need to allow modification bean properties to serialize:
            if (_factoryConfig.hasSerializerModifiers()) {
                for (BeanSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                    props = mod.changeProperties(serializationConfig, beanDesc, props);
                }
            }

            // Any properties to suppress?

            // 10-Dec-2021, tatu: [databind#3305] Some JDK types need special help
            //    (initially, `CharSequence` with its `isEmpty()` default impl)
            props = filterUnwantedJDKProperties(serializationConfig, beanDesc, props);
            props = filterBeanProperties(serializationConfig, beanDesc, props);

            // Need to allow reordering of properties to serialize
            if (_factoryConfig.hasSerializerModifiers()) {
                for (BeanSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                    props = mod.orderProperties(serializationConfig, beanDesc, props);
                }
            }
            return props;
        }


        protected List<BeanPropertyWriter> findBeanProperties(SerializerProvider prov, BeanDescription beanDesc)
                throws JsonMappingException {
            List<BeanPropertyDefinition> properties = beanDesc.findProperties();
            final SerializationConfig config = prov.getConfig();

            // ignore specified types
            removeIgnorableTypes(config, beanDesc, properties);

            // and possibly remove ones without matching mutator...
            if (config.isEnabled(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS)) {
                removeSetterlessGetters(config, beanDesc, properties);
            }

            // nothing? can't proceed (caller may or may not throw an exception)
            if (properties.isEmpty()) {
                return null;
            }
            // null is for value type serializer, which we don't have access to from here (ditto for bean prop)
            boolean staticTyping = usesStaticTyping(config, beanDesc);
            var pb = new PropertyBuilder(config, beanDesc);

            ArrayList<BeanPropertyWriter> result = new ArrayList<BeanPropertyWriter>(properties.size());
            for (BeanPropertyDefinition property : properties) {
                final AnnotatedMember accessor = property.getAccessor();
                // Type id? Requires special handling:
                if (property.isTypeId()) {
                    continue;
                }
                // suppress writing of back references
                AnnotationIntrospector.ReferenceProperty refType = property.findReferenceType();
                if (refType != null && refType.isBackReference()) {
                    continue;
                }
                if (accessor instanceof AnnotatedMethod) {
                    result.add(_constructWriter(prov, property, pb, staticTyping, (AnnotatedMethod) accessor));
                } else {
                    result.add(_constructWriter(prov, property, pb, staticTyping, (AnnotatedField) accessor));
                }
            }
            return result;
        }

        protected void removeIgnorableTypes(SerializationConfig config, BeanDescription beanDesc,
                                            List<BeanPropertyDefinition> properties) throws JsonMappingException {
            try {
                removeIgnorableTypes.invoke(COMMON.getSerializerFactory(), config, beanDesc, properties);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                reportMethodException(e);
            }
        }

        private static void reportMethodException(InvocationTargetException e) throws JsonMappingException {
            if (e.getTargetException() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (e.getTargetException() instanceof JsonMappingException io) {
                throw io;
            }
            throw new RuntimeException(e.getTargetException());
        }


        protected void removeOverlappingTypeIds(SerializerProvider prov, BeanDescription beanDesc, List<BeanPropertyWriter> props) {
            for (int i = 0, end = props.size(); i < end; ++i) {
                BeanPropertyWriter bpw = props.get(i);
                TypeSerializer td = bpw.getTypeSerializer();
                if ((td == null) || (td.getTypeInclusion() != JsonTypeInfo.As.EXTERNAL_PROPERTY)) {
                    continue;
                }
                String n = td.getPropertyName();
                PropertyName typePropName = PropertyName.construct(n);

                for (BeanPropertyWriter w2 : props) {
                    if ((w2 != bpw) && w2.wouldConflictWithName(typePropName)) {
                        bpw.assignTypeSerializer(null);
                        break;
                    }
                }
            }
        }

        protected void removeSetterlessGetters(SerializationConfig config, BeanDescription beanDesc,
                                               List<BeanPropertyDefinition> properties) {
            // one caveat: only remove implicit properties;
            // explicitly annotated ones should remain
            properties.removeIf(property -> !property.couldDeserialize() && !property.isExplicitlyIncluded());
        }

        @SuppressWarnings("unchecked")
        protected List<BeanPropertyWriter> filterUnwantedJDKProperties(SerializationConfig config,
                                                                       BeanDescription beanDesc, List<BeanPropertyWriter> props) throws JsonMappingException {
            try {
                return (List<BeanPropertyWriter>) filterUnwantedJDKProperties.invoke(COMMON.getSerializerFactory(), config, beanDesc, props);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                reportMethodException(e);
                throw Exit.exception();
            }
        }

        @SuppressWarnings("unchecked")
        protected List<BeanPropertyWriter> filterBeanProperties(SerializationConfig config,
                                                                BeanDescription beanDesc, List<BeanPropertyWriter> props) throws JsonMappingException {
            try {
                return (List<BeanPropertyWriter>) filterBeanProperties.invoke(COMMON.getSerializerFactory(), config, beanDesc, props);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                reportMethodException(e);
                throw Exit.exception();
            }
        }

        protected boolean usesStaticTyping(SerializationConfig config,
                                           BeanDescription beanDesc) throws JsonMappingException {
            try {
                return (boolean) usesStaticTyping.invoke(COMMON.getSerializerFactory(), config, beanDesc, null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                reportMethodException(e);
                throw new RuntimeException(e);
            }
        }

        protected BeanPropertyWriter _constructWriter(SerializerProvider prov,
                                                      BeanPropertyDefinition propDef,
                                                      PropertyBuilder pb, boolean staticTyping, AnnotatedMember accessor)
                throws JsonMappingException {
            try {
                return (BeanPropertyWriter) _constructWriter.invoke(COMMON.getSerializerFactory(), prov, propDef, pb, staticTyping, accessor);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                reportMethodException(e);
                throw new RuntimeException(e);
            }
        }

        private static YamlCommentGenerator createGenerator(Constructor<? extends YamlCommentGenerator> constructor, PropertyWriter propertyWriter) {
            try {
                return constructor.newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new SerializerException(STR."failed to initialize the common generator '\{constructor.getDeclaringClass()}'. position is '\{propertyWriter}'");
            }
        }

        private static Constructor<? extends YamlCommentGenerator> getGeneratorConstructor(Class<? extends YamlCommentGenerator> clazz) {
            try {
                var constructor = clazz.getConstructor();
                constructor.setAccessible(true);
                return constructor;
            } catch (NoSuchMethodException e) {
                throw new SerializerException(STR."\{clazz} should has a parameter-less constructor to support the work of annotion \{YamlComment.class}");
            }
        }
    }

    private static class CommentCombineMap extends LinkedHashMap<SerializableString, Object> {
        private final Map<SerializableString, List<String>> comments = new HashMap<>();

        private CommentSequencedSet sequencedSet;

        public void put(SerializableString key, Object content, List<String> comments) {
            this.comments.put(key, comments);
            super.put(key, content);
        }

        @Override
        public SequencedSet<Map.Entry<SerializableString, Object>> sequencedEntrySet() {
            if (sequencedSet == null) {
                sequencedSet = new CommentSequencedSet(super.sequencedEntrySet());
            }
            return sequencedSet;
        }

        class CommentSequencedSet implements SequencedSet<Map.Entry<SerializableString, Object>> {
            final SequencedSet<Map.Entry<SerializableString, Object>> rawSet;

            CommentSequencedSet(SequencedSet<Map.Entry<SerializableString, Object>> rawSet) {
                this.rawSet = rawSet;
            }

            @Override
            public SequencedSet<Map.Entry<SerializableString, Object>> reversed() {
                return new CommentSequencedSet(rawSet.reversed());
            }

            @Override
            public int size() {
                return rawSet.size() + comments.size();
            }

            @Override
            public boolean isEmpty() {
                return rawSet.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                return rawSet.contains(o);
            }

            @NotNull
            @Override
            public Iterator<Map.Entry<SerializableString, Object>> iterator() {
                return new CombineCommentIter(rawSet.iterator(), comments);
            }

            static class CombineCommentIter implements Iterator<Map.Entry<SerializableString, Object>> {

                private final Iterator<Map.Entry<SerializableString, Object>> rawItr;

                private final Map<SerializableString, List<String>> comments;
                private Map.Entry<SerializableString, Object> nowRaw;
                private Iterator<String> commentItr;

                private boolean lastIterated = false;

                CombineCommentIter(Iterator<Map.Entry<SerializableString, Object>> rawItr, Map<SerializableString, List<String>> comments) {
                    this.rawItr = rawItr;
                    this.comments = comments;
                }

                @Override
                public boolean hasNext() {
                    if (commentItr == null) {
                        return rawItr.hasNext();
                    }
                    if (commentItr.hasNext()) {
                        return true;
                    }
                    if (nowRaw == null) {
                        return rawItr.hasNext();
                    }
                    return !lastIterated;
                }

                @Override
                public Map.Entry<SerializableString, Object> next() {
                    if (commentItr != null && commentItr.hasNext()) {
                        return newEntry(commentItr.next());
                    }
                    if (nowRaw == null) {
                        nowRaw = rawItr.next();
                        var matchedComments = comments.get(nowRaw.getKey());
                        if (matchedComments == null || matchedComments.isEmpty()) {
                            return nowRaw;
                        }
                        commentItr = matchedComments.iterator();
                        return newEntry(commentItr.next());
                    }
                    var lastRaw = nowRaw;
                    if (lastIterated) {
                        throw new NoSuchElementException();
                    }
                    if (!rawItr.hasNext()) {
                        lastIterated = true;
                        return lastRaw;
                    }
                    nowRaw = rawItr.next();
                    var matchedComments = comments.get(nowRaw.getKey());
                    if (matchedComments == null || matchedComments.isEmpty()) {
                        return lastRaw;
                    }
                    commentItr = matchedComments.iterator();
                    return lastRaw;
                }

                @NotNull
                private static Map.Entry<SerializableString, Object> newEntry(Object comment) {
                    return new Map.Entry<>() {
                        static final SerializableString key = new SerializedString("#");

                        @Override
                        public SerializableString getKey() {
                            return key;
                        }

                        @Override
                        public Object getValue() {
                            return comment;
                        }

                        @Override
                        public Object setValue(Object value) {
                            return null;
                        }

                        @Override
                        public String toString() {
                            return STR."CommentEntry[\{key}:\{comment}]";
                        }
                    };
                }

                @Override
                public void remove() {
                    if (commentItr == null) {
                        throw new NoSuchElementException();
                    }
                    if (commentItr.hasNext()) {
                        return;
                    }
                    rawItr.remove();
                    comments.remove(nowRaw.getKey());
                }
            }

            @NotNull
            @Override
            public Object[] toArray() {
                return stream().toArray();
            }

            @NotNull
            @Override
            public <T> T @NotNull [] toArray(@NotNull T @NotNull [] a) {
                return stream().toArray(i -> a);
            }

            @Override
            public boolean add(Map.Entry<SerializableString, Object> stringObjectEntry) {
                return rawSet.add(stringObjectEntry);
            }

            @Override
            public boolean remove(Object o) {
                return rawSet.remove(o);
            }

            @Override
            public boolean containsAll(@NotNull Collection<?> c) {
                return rawSet.containsAll(c);
            }

            @Override
            public boolean addAll(@NotNull Collection<? extends Map.Entry<SerializableString, Object>> c) {
                return rawSet.addAll(c);
            }

            @Override
            public boolean removeAll(@NotNull Collection<?> c) {
                return rawSet.removeAll(c);
            }

            @Override
            public boolean retainAll(@NotNull Collection<?> c) {
                return rawSet.retainAll(c);
            }

            @Override
            public void clear() {
                rawSet.clear();
                comments.clear();
            }
        }
    }
}
