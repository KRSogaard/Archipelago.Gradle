package build.archipelago.common.clients.rest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public abstract class BaseRestClient {

    protected final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public BaseRestClient() {
        objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new Jdk8Module());
    }

    protected <T> T parseOrThrow(String body, Class<T> valueType) {
        try {
            return objectMapper.readValue(body, valueType);
        } catch (IOException e) {
            log.error(String.format("Failed to parse the string \"%s\" as a %s object", body, valueType.getName()), e);
            throw new RuntimeException("Failed to parse " + valueType.getName(), e);
        }
    }
}
