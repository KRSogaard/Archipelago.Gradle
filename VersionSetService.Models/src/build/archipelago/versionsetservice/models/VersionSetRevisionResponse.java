package build.archipelago.versionsetservice.models;

import lombok.*;

import java.util.List;

@Builder
@Value
public class VersionSetRevisionResponse {
    private Long created;
    private List<String> packages;
}
