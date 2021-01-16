package build.archipelago.packageservice.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadPackageRestRequest {
    private MultipartFile buildArtifact;
    private String gitCommit;
    private String gitBranch;
    private String config;
}
