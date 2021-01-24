package build.archipelago.buildserver.common.services.build.logs;

import build.archipelago.buildserver.models.BuildStage;
import build.archipelago.buildserver.models.exceptions.StageLogNotFoundException;

public interface StageLogsService {
    void uploadStageLog(String buildId, BuildStage buildStage, String readString);

    String getStageBuildLog(String accountId, String buildId, BuildStage buildStage) throws StageLogNotFoundException;
}
