package build.archipelago.packageservice.models;

import build.archipelago.common.git.models.GitCommit;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class GitCommitsResponse {
    private List<GitCommit> commits;
}
