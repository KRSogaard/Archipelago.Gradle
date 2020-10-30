package build.archipelago.common.exceptions;

import build.archipelago.common.exceptions.ArchipelagoException;

public class DependencyTransversalTypeNotFoundException extends ArchipelagoException {
    public DependencyTransversalTypeNotFoundException(String transversalType) {
        super("This transversal type \"" + transversalType + "\" was not found");
    }
}
