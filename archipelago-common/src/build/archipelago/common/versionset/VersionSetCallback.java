package build.archipelago.common.versionset;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Builder
@Value
public class VersionSetCallback {
    private String id;
    private String url;
}
