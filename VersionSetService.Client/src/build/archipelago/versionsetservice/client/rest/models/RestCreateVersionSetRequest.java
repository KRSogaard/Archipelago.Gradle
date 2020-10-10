package build.archipelago.versionsetservice.client.rest.models;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@Value
public class RestCreateVersionSetRequest {
    private String name;
    private List<String> targets;
    private String parent;
}
