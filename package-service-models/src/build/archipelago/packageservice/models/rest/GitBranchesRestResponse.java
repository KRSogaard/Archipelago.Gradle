package build.archipelago.packageservice.models.rest;

import build.archipelago.common.git.models.GitBranch;
import lombok.*;

import java.util.List;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class GitBranchesRestResponse {
    private List<GitBranchRestResponse> branches;
}
