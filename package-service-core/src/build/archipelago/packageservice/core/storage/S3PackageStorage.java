package build.archipelago.packageservice.core.storage;

import build.archipelago.common.ArchipelagoBuiltPackage;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Date;

@Slf4j
public class S3PackageStorage implements PackageStorage {

    private AmazonS3 s3Client;
    private String bucketName;

    public S3PackageStorage(AmazonS3 s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public void upload(String accountId, ArchipelagoBuiltPackage pkg, byte[] artifactBytes) {
        String keyName = this.getS3FileName(accountId, pkg);
        log.info("Saving build artifact to '{}'", keyName);

        ObjectMetadata om = new ObjectMetadata();
        om.setContentLength(artifactBytes.length);

        s3Client.putObject(
                new PutObjectRequest(bucketName, keyName, new ByteArrayInputStream(artifactBytes), om));
    }

    @Override
    public String getDownloadUrl(String accountId, ArchipelagoBuiltPackage pkg) {
        String keyName = this.getS3FileName(accountId, pkg);
        log.debug("Fetching build artifact from S3 '{}' with key '{}'", bucketName, keyName);
        // The user have 5 min to download the file
        Instant expiresAt = Instant.now().plusSeconds(60 * 5);
        URL url = s3Client.generatePresignedUrl(bucketName, keyName, Date.from(expiresAt));
        return url.toString();
    }

    private String getS3FileName(String accountId, ArchipelagoBuiltPackage pkg) {
        return accountId + "/" + pkg.getName() + "/" + pkg.getVersion() + "/" + pkg.getHash() + ".zip";
    }
}
