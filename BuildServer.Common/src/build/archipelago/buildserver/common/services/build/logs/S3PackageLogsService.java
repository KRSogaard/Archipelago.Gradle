package build.archipelago.buildserver.common.services.build.logs;

import build.archipelago.buildserver.models.exceptions.PackageLogNotFoundException;
import build.archipelago.common.ArchipelagoPackage;
import com.amazonaws.services.s3.AmazonS3;
import com.google.common.base.*;

import java.net.URL;
import java.time.Instant;
import java.util.Date;

public class S3PackageLogsService implements PackageLogsService {
    private AmazonS3 amazonS3;
    private String packageBuildLogS3Bucket;

    public S3PackageLogsService(AmazonS3 amazonS3, String packageBuildLogS3Bucket) {
        this.amazonS3 = amazonS3;
        this.packageBuildLogS3Bucket = packageBuildLogS3Bucket;
    }

    @Override
    public void uploadLog(String accountId, String buildId, ArchipelagoPackage pkg, String content) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(pkg);

        String S3Key = this.getKey(accountId, buildId, pkg);
        amazonS3.putObject(packageBuildLogS3Bucket, S3Key, content);
    }

    @Override
    public String getPackageBuildLog(String accountId, String buildId, ArchipelagoPackage pkg) throws PackageLogNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(pkg);

        String S3Key = this.getKey(accountId, buildId, pkg);

        boolean exists = amazonS3.doesObjectExist(packageBuildLogS3Bucket, S3Key);
        if (!exists) {
            throw new PackageLogNotFoundException(buildId, pkg);
        }

        Instant expiresAt = Instant.now().plusSeconds(60 * 5);
        URL url = amazonS3.generatePresignedUrl(packageBuildLogS3Bucket, S3Key, Date.from(expiresAt));
        return url.toString();
    }

    private String getKey(String accountId, String buildId, ArchipelagoPackage pkg) {
        return accountId.toLowerCase() + "/" + buildId.toLowerCase() + "/" + pkg.getNameVersion().toLowerCase() + ".log";
    }
}
