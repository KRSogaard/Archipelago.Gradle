package build.archipelago.account.common.exceptions;

import build.archipelago.common.exceptions.ArchipelagoException;

public class AccountNotFoundException extends ArchipelagoException {
    public AccountNotFoundException(String accountId) {
        super("The account with id \"" + accountId + "\" was not found");
    }
}
