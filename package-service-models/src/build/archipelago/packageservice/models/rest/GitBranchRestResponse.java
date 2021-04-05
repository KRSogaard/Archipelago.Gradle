package build.archipelago.packageservice.models.rest;

import build.archipelago.common.git.models.GitBranch;
import build.archipelago.common.git.models.GitCommit;
import lombok.*;

import java.time.Instant;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class GitBranchRestResponse {
    private String sha;
    private String name;

    public static GitBranchRestResponse from(GitBranch gitBranch) {
        return GitBranchRestResponse.builder()
                .name(gitBranch.getName())
                .sha(gitBranch.getSha())
                .build();
    }

    public GitBranch toInternal() {
        return GitBranch.builder()
                .name(getName())
                .sha(getSha())
                .build();
    }
}