package build.archipelago.common.git.models.exceptions;

public class GitDetailsNotFound extends Exception {
    public GitDetailsNotFound(String accountId) {
        super("Git details for account with id \"" + accountId + "\" was not found");
    }
    public GitDetailsNotFound() {
        super("Git details was not found");
    }
}
