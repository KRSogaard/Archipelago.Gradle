package build.archipelago.buildserver.common.services.build.logs;

import build.archipelago.buildserver.models.exceptions.PackageLogNotFoundException;
import build.archipelago.common.ArchipelagoPackage;

public interface PackageLogsService {
    void uploadLog(String accountId, String buildId, ArchipelagoPackage pkgName, String content);

    String getPackageBuildLog(String accountId, String buildId, ArchipelagoPackage pkgName) throws PackageLogNotFoundException;
}
