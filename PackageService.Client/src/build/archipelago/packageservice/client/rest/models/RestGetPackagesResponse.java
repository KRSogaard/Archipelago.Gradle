package build.archipelago.packageservice.client.rest.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestGetPackagesResponse {
    private List<RestGetPackageResponse> packages;
}
