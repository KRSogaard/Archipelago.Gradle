package build.archipelago.maui.core.exceptions;

public class RecipeNotFoundException extends Exception {
    public RecipeNotFoundException(String recipe) {
        super("The recipe \"" + recipe + "\" was not found");
    }
}
