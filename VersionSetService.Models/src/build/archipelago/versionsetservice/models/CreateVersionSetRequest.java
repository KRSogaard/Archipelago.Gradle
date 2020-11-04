package build.archipelago.versionsetservice.models;

import com.google.common.base.*;
import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateVersionSetRequest {
    private String name;
    private List<String> targets;
    private String parent;

    public void validate() throws IllegalArgumentException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Name is required");
        Preconditions.checkNotNull(targets, "At least 1 target is required");
        Preconditions.checkArgument(targets.size() > 0, "At least 1 target is required");
    }
}
