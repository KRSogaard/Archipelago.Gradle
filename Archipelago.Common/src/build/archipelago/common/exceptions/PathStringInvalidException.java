package build.archipelago.common.exceptions;

import build.archipelago.common.exceptions.ArchipelagoException;

public class PathStringInvalidException extends ArchipelagoException {
    public PathStringInvalidException(String pathString) {
        super("This path string \"" + pathString + "\" was invalid");
    }
}