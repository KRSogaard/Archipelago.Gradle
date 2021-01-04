package build.archipelago.packageservice.client.rest.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ArchipelagoBuiltPackageResponse {
    private String name;
    private String version;
    private String hash;
}
