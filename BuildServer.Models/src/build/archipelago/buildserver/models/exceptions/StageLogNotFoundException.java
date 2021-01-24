package build.archipelago.buildserver.models.exceptions;

import build.archipelago.buildserver.models.BuildStage;
import lombok.*;

@Builder
@Getter
public class StageLogNotFoundException extends Exception {
    private String buildId;
    private BuildStage stage;

    public StageLogNotFoundException(String buildId, BuildStage stage) {
        super("Build log file the sage '" + stage + "' in build '" + buildId + "' was not found");
        this.buildId = buildId;
        this.stage = stage;
    }
}
