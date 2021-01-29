package build.archipelago.packageservice.models.rest;

import lombok.*;

import java.util.List;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class GetPackagesRestResponse {
    private List<GetPackageRestResponse> packages;
}
