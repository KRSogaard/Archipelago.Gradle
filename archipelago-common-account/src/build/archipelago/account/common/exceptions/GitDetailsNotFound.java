package build.archipelago.account.common.exceptions;

public class GitDetailsNotFound extends Exception {
    public GitDetailsNotFound(String accountId) {
        super("Git details for account with id \"" + accountId + "\" was not found");
    }
}
