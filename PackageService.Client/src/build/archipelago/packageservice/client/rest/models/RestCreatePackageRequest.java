package build.archipelago.packageservice.client.rest.models;

import lombok.*;

@AllArgsConstructor
@Value
public class RestCreatePackageRequest {
    private String name;
    private String description;
}
