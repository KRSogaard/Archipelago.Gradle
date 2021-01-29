package build.archipelago.harbor.client.models;

import lombok.*;

@Builder
@Value
public class CreatePackageRequest {
    private String name;
    private String description;
}
