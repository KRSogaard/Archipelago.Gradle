package build.archipelago.buildserver.models.exceptions;

import lombok.*;

@Builder
@Getter
public class BuildNotFoundException extends Exception {
    private String buildId;

    public BuildNotFoundException(String buildId) {
        super("Build '" + buildId + "' was not found");
        this.buildId = buildId;
    }
}
