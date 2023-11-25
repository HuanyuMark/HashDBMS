package org.hashdb.ms.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hashdb.ms.exception.DBInnerException;

/**
 * Date: 2023/11/24 23:13
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class JacksonSerializer {
    public static final ObjectMapper COMMON = new ObjectMapper();

    public static String jsonfy(Object obj) {
        try {
            return COMMON.writeValueAsString(obj);
        } catch (Exception e) {
            throw new DBInnerException(e);
        }
    }

    public static Object parse(String json, Class<?> clazz) throws JsonProcessingException {
        return COMMON.readValue(json, clazz);
    }

    public static Object parse(String json) throws JsonProcessingException {
        return COMMON.readValue(json, Object.class);
    }
}
