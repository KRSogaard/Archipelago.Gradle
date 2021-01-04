package build.archipelago.packageservice.models;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ArchipelagoBuiltPackageRestResponse {
    private String name;
    private String version;
    private String hash;
}
