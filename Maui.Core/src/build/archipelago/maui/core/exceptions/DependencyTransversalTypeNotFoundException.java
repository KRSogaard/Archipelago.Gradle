package build.archipelago.maui.core.exceptions;

public class DependencyTransversalTypeNotFoundException extends Exception {
    public DependencyTransversalTypeNotFoundException(String transversalType) {
        super("This transversal type \"" + transversalType + "\" was not found");
    }
}
