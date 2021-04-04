package build.archipelago.buildserver.builder.notifications;

import build.archipelago.buildserver.models.BuildPackageDetails;

import java.util.List;

public interface NotificationProvider {
    void buildStarted(String buildId, String accountId, String versionSet, List<BuildPackageDetails> buildPackages, boolean dryRun);

    void stageStarted(String buildId, String accountId, String stage);

    void stageFinished(String buildId, String accountId, String stage);

    void stageFailed(String buildId, String accountId, String stage);

    void buildSuccessful(String buildId, String accountId);

    void buildFailed(String buildId, String accountId);

    void buildError(String buildId, String accountId, RuntimeException exp);

    void buildDone(String buildId, String accountId);
}
