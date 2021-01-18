package build.archipelago.packageservice.models.rest;

import build.archipelago.packageservice.models.GetBuildArtifactResponse;
import lombok.*;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class GetBuildArtifactRestResponse {
    private String url;
    private String fileName;
    private String hash;

    public static GetBuildArtifactRestResponse from(GetBuildArtifactResponse response) {
        return GetBuildArtifactRestResponse.builder()
                .fileName(response.getFileName())
                .hash(response.getHash())
                .url(response.getUrl())
                .build();
    }

    public GetBuildArtifactResponse toInternal() {
        return GetBuildArtifactResponse.builder()
                .fileName(this.getFileName())
                .hash(this.getHash())
                .url(this.getUrl())
                .build();
    }
}
