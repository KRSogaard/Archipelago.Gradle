package build.archipelago.common.git.models;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Builder
@Getter
public class GitCommit {
    private String sha;
    private String message;
    private String author;
    private Instant created;
}
