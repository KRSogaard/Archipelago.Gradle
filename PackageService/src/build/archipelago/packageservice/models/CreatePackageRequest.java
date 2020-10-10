package build.archipelago.packageservice.models;

import lombok.*;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class CreatePackageRequest {
    private String name;
    private String description;
}
