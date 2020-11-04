package build.archipelago.packageservice.models;

import lombok.*;

@Builder
@Value
public class ArchipelagoBuiltPackageResponse {
    private String name;
    private String version;
    private String hash;
}
