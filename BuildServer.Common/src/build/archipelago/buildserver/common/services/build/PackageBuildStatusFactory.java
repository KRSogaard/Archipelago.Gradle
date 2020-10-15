package build.archipelago.buildserver.common.services.build;

import com.amazonaws.services.s3.AmazonS3;

public class PackageBuildStatusFactory {
    private AmazonS3 amazonS3;
    private String bucketName;

    public PackageBuildStatusFactory(AmazonS3 amazonS3, String bucketName) {
        this.amazonS3 = amazonS3;
        this.bucketName = bucketName;
    }

    public PackageBuildStatus createPackageBuildStatus(String buildId) {
        return new PackageBuildStatus(amazonS3, bucketName, buildId);
    }
}
