package build.archipelago.packageservice.client.models;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class GetPackagesResponse {
    private ImmutableList<GetPackageResponse> packages;
}
