package build.archipelago.buildserver.builder.output;

import build.archipelago.maui.core.output.OutputWrapper;
import com.amazonaws.services.s3.AmazonS3;

import java.time.Instant;
import java.util.ArrayList;

public class S3OutputWrapper implements OutputWrapper {

    private AmazonS3 amazonS3;
    private String packageBuildLogS3Bucket;
    private String packageBuildLogS3File;

    private final StringBuilder logs;
    private boolean hasLogs;


    public S3OutputWrapper(AmazonS3 amazonS3, String packageBuildLogS3Bucket, String packageBuildLogS3File) {
        this.amazonS3 = amazonS3;
        this.packageBuildLogS3Bucket = packageBuildLogS3Bucket;
        this.packageBuildLogS3File = packageBuildLogS3File;

        logs = new StringBuilder();
    }

    @Override
    public void write(String message) {
        write(message, new ArrayList<String>());
    }

    @Override
    public void write(String message, Object... args) {
        addMessage("OUT", message, args);
    }

    @Override
    public void error(String message) {
        error(message, new ArrayList<String>());
    }

    @Override
    public void error(String message, Object... args) {
        addMessage("ERROR", message, args);
    }

    public void upload() {
        amazonS3.putObject(packageBuildLogS3Bucket, packageBuildLogS3File, logs.toString());
    }

    private void addMessage(String prefix, String message, Object... args) {
        synchronized (logs) {
            hasLogs = true;
            logs.append(Instant.now().toEpochMilli());
            logs.append(";");
            logs.append(prefix);
            logs.append(": ");
            logs.append(String.format(message, args));
            logs.append("\n");
        }
    }
}
