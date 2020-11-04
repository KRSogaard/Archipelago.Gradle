package build.archipelago.common.exceptions;

import build.archipelago.common.exceptions.ArchipelagoException;

public class RecipeNotFoundException extends ArchipelagoException {
    public RecipeNotFoundException(String recipe) {
        super("The recipe \"" + recipe + "\" was not found");
    }
}