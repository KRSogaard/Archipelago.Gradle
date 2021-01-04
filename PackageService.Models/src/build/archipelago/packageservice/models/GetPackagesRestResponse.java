package build.archipelago.packageservice.models;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GetPackagesRestResponse {
    private List<GetPackageRestResponse> packages;
}
