package build.archipelago.packageservice.models.rest;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
                .packages(packages.stream().map(ArchipelagoPackage::getNameVersion).collect(Collectors.toList()))
                .build();
    }
    public static VerificationRestRequest fromBuilt(List<ArchipelagoBuiltPackage> packages) {
        return VerificationRestRequest.builder()
                .packages(packages.stream().map(ArchipelagoBuiltPackage::getNameVersion).collect(Collectors.toList()))
                .build();
    }
}
