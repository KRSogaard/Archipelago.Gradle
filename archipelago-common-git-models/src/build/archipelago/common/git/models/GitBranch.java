package build.archipelago.common.git.models;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GitBranch {
    private String name;
    private String sha;
}
