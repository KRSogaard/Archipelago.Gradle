package build.archipelago.packageservice.client.models;

import lombok.*;

@Builder
@Value
public class CreatePackageRequest {
    private String name;
    private String description;
}
