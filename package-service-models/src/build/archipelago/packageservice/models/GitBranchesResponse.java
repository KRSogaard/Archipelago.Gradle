package build.archipelago.packageservice.models;

import build.archipelago.common.git.models.GitBranch;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class GitBranchesResponse {
    private List<GitBranch> branches;
}
