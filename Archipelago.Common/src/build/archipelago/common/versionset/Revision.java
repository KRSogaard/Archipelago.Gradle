package build.archipelago.common.versionset;

import lombok.*;

import java.time.Instant;

@Builder
@Value
public class Revision {
    private String revisionId;
    private Instant created;
}
