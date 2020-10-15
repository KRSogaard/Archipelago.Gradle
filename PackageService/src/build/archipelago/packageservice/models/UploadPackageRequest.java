package build.archipelago.packageservice.models;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadPackageRequest {
    private MultipartFile buildArtifact;
    private String gitCommit;
    private String gitBranch;
    private String config;
}
