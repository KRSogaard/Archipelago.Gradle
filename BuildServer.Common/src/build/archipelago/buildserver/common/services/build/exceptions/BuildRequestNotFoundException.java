package build.archipelago.buildserver.common.services.build.exceptions;

public class BuildRequestNotFoundException extends Exception {
    public BuildRequestNotFoundException(String buildId) {
        super("Build if \"" + buildId + "\" was not found");
    }
}
