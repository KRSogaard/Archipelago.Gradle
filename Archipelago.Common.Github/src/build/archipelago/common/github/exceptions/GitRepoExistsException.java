package build.archipelago.common.github.exceptions;

public class GitRepoExistsException extends Exception {
    public GitRepoExistsException(String name) {
        super(name);
    }
}
