package build.archipelago.common.exceptions;

public class PathStringInvalidException extends ArchipelagoException {
    public PathStringInvalidException(String pathString) {
        super("This path string \"" + pathString + "\" was invalid");
    }
}
