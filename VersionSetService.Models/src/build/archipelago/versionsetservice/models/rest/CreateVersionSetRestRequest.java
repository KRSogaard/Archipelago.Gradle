package build.archipelago.versionsetservice.models.rest;

import com.google.common.base.*;
import lombok.*;

import java.util.List;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class CreateVersionSetRestRequest {
    private String name;
    private List<String> targets;
    private String parent;

    public void validate() throws IllegalArgumentException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Name is required");
    }
}
