package build.archipelago.versionsetservice.models.rest;

import com.google.common.base.Preconditions;
import lombok.*;

import java.util.List;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class CreateVersionSetRevisionRestRequest {
    private List<String> packages;
    private String target;

    public void validate() {
        Preconditions.checkNotNull(packages, "Packages are required");
        Preconditions.checkArgument(packages.size() > 0, "A minimum of 1 package is required");
    }
}
