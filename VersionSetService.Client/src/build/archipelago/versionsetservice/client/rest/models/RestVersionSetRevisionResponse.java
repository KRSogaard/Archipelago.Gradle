package build.archipelago.versionsetservice.client.rest.models;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestVersionSetRevisionResponse {
    private Long created;
    private List<String> packages;
}
