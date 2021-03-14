package build.archipelago.account.common.exceptions;

import build.archipelago.common.exceptions.ArchipelagoException;

public class AccountExistsException extends ArchipelagoException {
    public AccountExistsException(String accountId) {
        super("The account with id \"" + accountId + "\" already exists");
    }
}
