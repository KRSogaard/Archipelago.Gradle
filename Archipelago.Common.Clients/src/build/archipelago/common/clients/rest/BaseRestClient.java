package build.archipelago.common.clients.rest;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public abstract class BaseRestClient {

    protected com.fasterxml.jackson.databind.ObjectMapper objectMapper
            = new com.fasterxml.jackson.databind.ObjectMapper();

    protected <T> T parseOrThrow(String body, Class<T> valueType) {
        try {
            return objectMapper.readValue(body, valueType);
        } catch (IOException e) {
            log.error(String.format("Failed to parse the string \"%s\" as a %s object", body, valueType.getName()), e);
            throw new RuntimeException("Failed to parse " + valueType.getName(), e);
        }
    }
}
