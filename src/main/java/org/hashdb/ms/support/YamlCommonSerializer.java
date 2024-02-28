package org.hashdb.ms.support;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;

import java.io.IOException;
import java.util.Set;

/**
 * Date: 2024/2/23 17:21
 *
 * @author Huanyu Mark
 * @version 0.0.1
 */
public class YamlCommonSerializer extends BeanSerializer {

    @Override
    protected void serializeFields(Object bean, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        final BeanPropertyWriter[] props;
        if (_filteredProps != null && provider.getActiveView() != null) {
            props = _filteredProps;
        } else {
            props = _props;
        }
        int i = 0;
        try {
            for (final int len = props.length; i < len; ++i) {
                BeanPropertyWriter prop = props[i];
                if (prop != null) { // can have nulls in filtered list
                    YamlComment yamlComment = prop.getAnnotation(YamlComment.class);
                    prop.serializeAsField(bean, gen, provider);
                }
            }
            if (_anyGetterWriter != null) {
                _anyGetterWriter.getAndSerialize(bean, gen, provider);
            }
        } catch (Exception e) {
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            wrapAndThrow(provider, e, bean, name);
        } catch (StackOverflowError e) {
            // 04-Sep-2009, tatu: Dealing with this is tricky, since we don't have many
            //   stack frames to spare... just one or two; can't make many calls.

            // 10-Dec-2015, tatu: and due to above, avoid "from" method, call ctor directly:
            //JsonMappingException mapE = JsonMappingException.from(gen, "Infinite recursion (StackOverflowError)", e);
            DatabindException mapE = new JsonMappingException(gen, "Infinite recursion (StackOverflowError)", e);

            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            mapE.prependPath(bean, name);
            throw mapE;
        }
    }

    public YamlCommonSerializer(JavaType type, BeanSerializerBuilder builder, BeanPropertyWriter[] properties, BeanPropertyWriter[] filteredProperties) {
        super(type, builder, properties, filteredProperties);
    }

    public YamlCommonSerializer(BeanSerializerBase src) {
        super(src);
    }

    public YamlCommonSerializer(BeanSerializerBase src, ObjectIdWriter objectIdWriter) {
        super(src, objectIdWriter);
    }

    public YamlCommonSerializer(BeanSerializerBase src, ObjectIdWriter objectIdWriter, Object filterId) {
        super(src, objectIdWriter, filterId);
    }

    public YamlCommonSerializer(BeanSerializerBase src, Set<String> toIgnore, Set<String> toInclude) {
        super(src, toIgnore, toInclude);
    }

    public YamlCommonSerializer(BeanSerializerBase src, BeanPropertyWriter[] properties, BeanPropertyWriter[] filteredProperties) {
        super(src, properties, filteredProperties);
    }
}
