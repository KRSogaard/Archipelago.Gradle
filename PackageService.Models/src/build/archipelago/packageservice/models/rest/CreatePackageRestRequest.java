package build.archipelago.packageservice.models.rest;

import lombok.*;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class CreatePackageRestRequest {
    private String name;
    private String description;
}
