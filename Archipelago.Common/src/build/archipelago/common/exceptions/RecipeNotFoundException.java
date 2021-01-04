package build.archipelago.common.exceptions;

public class RecipeNotFoundException extends ArchipelagoException {
    public RecipeNotFoundException(String recipe) {
        super("The recipe \"" + recipe + "\" was not found");
    }
}
