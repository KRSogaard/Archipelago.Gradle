package build.archipelago.packageservice.core.storage;

import build.archipelago.common.ArchipelagoBuiltPackage;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

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
        String keyName = getS3FileName(accountId, pkg);
        log.info("Saving build artifact to \"{}\"", keyName);

        ObjectMetadata om = new ObjectMetadata();
        om.setContentLength(artifactBytes.length);

        s3Client.putObject(
            new PutObjectRequest(bucketName, keyName, new ByteArrayInputStream(artifactBytes), om));
    }

    @Override
    public byte[] get(String accountId, ArchipelagoBuiltPackage pkg) throws IOException {
        String keyName = getS3FileName(accountId, pkg);
        log.debug("Fetching build artifact from S3 \"{}\" with key \"{}\"", bucketName, keyName);
        S3Object result = s3Client.getObject(bucketName, keyName);
        // TODO: Check is object exists
        try {
            return IOUtils.toByteArray(result.getObjectContent());
        } catch (AmazonServiceException exp) {
            log.error("Was not able to download the S3 file {}", keyName);
            throw exp; // This should not happen, so it is ok to throw so we can fail fast
        }
    }

    private String getS3FileName(String accountId, ArchipelagoBuiltPackage pkg) {
        return accountId + "/" + pkg.getName() + "/" + pkg.getVersion() + "/" + pkg.getHash() + ".zip";
    }
}
