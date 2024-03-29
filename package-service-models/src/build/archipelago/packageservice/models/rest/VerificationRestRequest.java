package build.archipelago.packageservice.models.rest;

import build.archipelago.common.*;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class VerificationRestRequest {
    private List<String> packages;

    public static VerificationRestRequest from(List<ArchipelagoPackage> packages) {
        return VerificationRestRequest.builder()
                .packages(packages.stream().map(ArchipelagoPackage::toString).collect(Collectors.toList()))
                .build();
    }

    public static VerificationRestRequest fromBuilt(List<ArchipelagoBuiltPackage> packages) {
        List<String> pkgs = packages.stream().map(x -> {
            String name = x.getBuiltPackageName();
            return name;
        }).collect(Collectors.toList());
        return VerificationRestRequest.builder()
                .packages(pkgs)
                .build();
    }
}
