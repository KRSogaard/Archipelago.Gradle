package build.archipelago.packageservice.client.models;

import build.archipelago.common.ArchipelagoPackage;
import lombok.*;

@Builder
@Value
public class UploadPackageRequest {
    private ArchipelagoPackage pkg;
    private String config;
    private String gitCommit;
    private String gitBranch;
}
