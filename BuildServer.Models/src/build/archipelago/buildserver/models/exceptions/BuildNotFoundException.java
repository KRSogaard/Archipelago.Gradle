package build.archipelago.buildserver.common.services.build.exceptions;

public class BuildNotFoundException extends Exception {
    private String buildId;

    public BuildNotFoundException(String buildId) {
        super("Build '" + buildId + "' was not found");
        this.buildId = buildId;
    }
}
