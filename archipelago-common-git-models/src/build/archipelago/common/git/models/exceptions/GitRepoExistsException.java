package build.archipelago.common.git.models.exceptions;

public class GitRepoExistsException extends Exception {
    public GitRepoExistsException(String name) {
        super(name);
    }
}
