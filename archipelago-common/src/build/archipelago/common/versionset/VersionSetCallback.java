package build.archipelago.common.versionset;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class VersionSetCallback {
    private String id;
    private String url;
}
