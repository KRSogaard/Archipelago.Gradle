package build.archipelago.common.git.models.exceptions;

import lombok.*;

@Getter
public class RepoNotFoundException extends Exception {
    private String repo;

    public RepoNotFoundException(String repo) {
        super();
        this.repo = repo;
    }
}
