package build.archipelago.versionsetservice.client.rest.models;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@Value
public class RestCreateVersionSetRevisionRequest {
    private List<String> packages;
}
