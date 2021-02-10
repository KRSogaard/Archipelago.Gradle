package build.archipelago.authservice.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import java.util.HashMap;

public class JSONUtil {
    private static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.registerModule(new Jdk8Module());
    }

    public static String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T deserialize(String head, Class<T> klass) {
        try {
            return objectMapper.readValue(head, klass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
