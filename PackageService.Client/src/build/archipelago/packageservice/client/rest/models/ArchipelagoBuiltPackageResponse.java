package build.archipelago.packageservice.client.rest.models;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ArchipelagoBuiltPackageResponse {
    private String name;
    private String version;
    private String hash;
}
