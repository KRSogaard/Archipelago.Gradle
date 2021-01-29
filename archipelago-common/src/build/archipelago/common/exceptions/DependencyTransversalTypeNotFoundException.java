package build.archipelago.common.exceptions;

public class DependencyTransversalTypeNotFoundException extends ArchipelagoException {
    public DependencyTransversalTypeNotFoundException(String transversalType) {
        super("This transversal type \"" + transversalType + "\" was not found");
    }
}
