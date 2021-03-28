package build.archipelago.packageservice.models.rest;

import lombok.*;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ArtifactUploadRestResponse {
    private String hash;
    private String uploadUrl;
}
