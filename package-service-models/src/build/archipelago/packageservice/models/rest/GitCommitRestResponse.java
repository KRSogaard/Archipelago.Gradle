package build.archipelago.packageservice.models.rest;


import build.archipelago.common.git.models.GitCommit;
import lombok.*;

import java.time.Instant;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class GitCommitRestResponse {
    private String sha;
    private String message;
    private String author;
    private Long created;

    public static GitCommitRestResponse from(GitCommit commit) {
        return GitCommitRestResponse.builder()
                .sha(commit.getSha())
                .message(commit.getMessage())
                .author(commit.getAuthor())
                .created(commit.getCreated().toEpochMilli())
                .build();
    }

    public GitCommit toInternal() {
        return GitCommit.builder()
                .sha(getSha())
                .message(getMessage())
                .author(getAuthor())
                .created(Instant.ofEpochMilli(getCreated()))
                .build();
    }
}
