package build.archipelago.buildserver.builder.output;

import build.archipelago.buildserver.common.services.build.logs.PackageLogsService;
import build.archipelago.common.ArchipelagoPackage;
import com.google.common.base.*;

public class S3OutputWrapperFactory {
    private PackageLogsService service;

    public S3OutputWrapperFactory(PackageLogsService service) {
        this.service = service;
    }

    public S3OutputWrapper create(String accountId, String buildId, ArchipelagoPackage pkg) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(pkg);

        return new S3OutputWrapper(service, accountId, buildId, pkg);
    }
}
