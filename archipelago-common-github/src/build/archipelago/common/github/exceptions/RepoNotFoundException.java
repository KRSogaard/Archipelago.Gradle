package build.archipelago.common.github.exceptions;

import lombok.*;

@Getter
public class RepoNotFoundException extends Exception {
    private String repo;

    public RepoNotFoundException(String repo) {
        super();
        this.repo = repo;
    }
}
