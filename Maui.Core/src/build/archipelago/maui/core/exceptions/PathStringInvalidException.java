package build.archipelago.maui.core.exceptions;

public class PathStringInvalidException extends Exception {
    public PathStringInvalidException(String pathString) {
        super("This path string \"" + pathString + "\" was invalid");
    }
}
