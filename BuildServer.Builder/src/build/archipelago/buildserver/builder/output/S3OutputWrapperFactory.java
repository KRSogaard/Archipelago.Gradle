package build.archipelago.buildserver.builder.output;

import com.amazonaws.services.s3.AmazonS3;

public class S3OutputWrapperFactory {
    private AmazonS3 amazonS3;
    private String packageBuildLogS3Bucket;

    public S3OutputWrapperFactory(AmazonS3 amazonS3, String packageBuildLogS3Bucket) {
        this.amazonS3 = amazonS3;
        this.packageBuildLogS3Bucket = packageBuildLogS3Bucket;
    }

    public S3OutputWrapper create(String accountId, String buildId, String pkgName) {
        String key = accountId + "/" + buildId + "/" + pkgName + ".log";
        return new S3OutputWrapper(amazonS3, packageBuildLogS3Bucket, key);
    }
}
