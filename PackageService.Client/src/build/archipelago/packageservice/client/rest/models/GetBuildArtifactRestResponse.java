package build.archipelago.packageservice.client.rest.models;

import build.archipelago.packageservice.client.models.GetPackageResponse;
import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetBuildArtifactRestResponse {
    private String url;
    private String fileName;
    private String hash;
}
