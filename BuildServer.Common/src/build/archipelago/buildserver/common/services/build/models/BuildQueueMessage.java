package build.archipelago.buildserver.common.services.build.models;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.io.IOException;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BuildQueueMessage {
    private static final ObjectMapper mapper = new ObjectMapper(new JsonFactory());

    private String buildId;

    public String toJson() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static BuildQueueMessage parse(String message) {
        try {
            return mapper.readValue(message, BuildQueueMessage.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}