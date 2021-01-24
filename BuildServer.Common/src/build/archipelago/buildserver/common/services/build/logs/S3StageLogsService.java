package build.archipelago.buildserver.common.services.build.logs;

import build.archipelago.buildserver.models.BuildStage;
import build.archipelago.buildserver.models.exceptions.StageLogNotFoundException;
import com.amazonaws.services.s3.AmazonS3;
import com.google.common.base.*;

import java.net.URL;
import java.time.Instant;
import java.util.Date;

public class S3StageLogsService implements StageLogsService {
    private AmazonS3 amazonS3;
    private String packageBuildLogS3Bucket;

    public S3StageLogsService(AmazonS3 amazonS3, String packageBuildLogS3Bucket) {
        this.amazonS3 = amazonS3;
        this.packageBuildLogS3Bucket = packageBuildLogS3Bucket;
    }

    @Override
    public void uploadStageLog(String buildId, BuildStage buildStage, String readString) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkNotNull(buildStage);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(readString));

        amazonS3.putObject(packageBuildLogS3Bucket, this.getS3LogKey(buildId, buildStage.getStage()), readString);
    }

    @Override
    public String getStageBuildLog(String accountId, String buildId, BuildStage buildStage) throws StageLogNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(buildStage);

        String S3Key = this.getS3LogKey(buildId, buildStage.getStage());

        boolean exists = amazonS3.doesObjectExist(packageBuildLogS3Bucket, S3Key);
        if (!exists) {
            throw new StageLogNotFoundException(buildId, buildStage);
        }

        Instant expiresAt = Instant.now().plusSeconds(60 * 5);
        URL url = amazonS3.generatePresignedUrl(packageBuildLogS3Bucket, S3Key, Date.from(expiresAt));
        return url.toString();
    }

    private String getS3LogKey(String buildId, String nameVersion) {
        return buildId.toLowerCase() + "/" + nameVersion.toLowerCase() + ".log";
    }
}
