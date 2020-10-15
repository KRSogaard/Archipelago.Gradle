package build.archipelago.buildserver.common.services.build;

import com.amazonaws.services.s3.AmazonS3;

import java.time.Instant;
import java.util.*;

public class PackageBuildStatus {
    private AmazonS3 amazonS3;
    private String buildId;
    private String bucketName;

    private Map<String, List<String>> outputLines;
    private Map<String, Instant> lastWrite;
    private Map<String, Integer> lastId;

    public PackageBuildStatus(AmazonS3 amazonS3, String bucketName, String buildId) {
        this.amazonS3 = amazonS3;
        this.buildId = buildId.toLowerCase();

        outputLines = new HashMap<>();
        lastWrite = new HashMap<>();
        lastId = new HashMap<>();
    }

    public void addBuildOutput(String packageId, OutputType type, String line) {
        String key = packageId.toLowerCase();
        if (!outputLines.containsKey(key)) {
            outputLines.put(key, new ArrayList<>());
        }

        outputLines.get(key).add(getLine(type, line));
        writeIfNeeded(packageId);
    }

    private void writeIfNeeded(String key) {
        if (!outputLines.containsKey(key)) {
            outputLines.put(key, new ArrayList<>());
        }
        if (!lastWrite.containsKey(key)) {
            lastWrite.put(key, Instant.now());
        }

        if ((Instant.now().minusSeconds(30).isAfter(lastWrite.get(key)) ||
                outputLines.get(key).size() > 100) &&
                outputLines.get(key).size() > 0) {
            writeStatusForPackage(key);
        }
    }

    public void finishPackage(String packageId) {
        String key = packageId.toLowerCase();
        writeStatusForPackage(key);
    }

    private void writeStatusForPackage(String key) {
        int nextId = 1;
        if (lastId.containsKey(key)) {
            nextId = lastId.get(key) + 1;
        }

        String output = getFileOutput(key);
        if (output == null) {
            return;
        }
        outputLines.get(key).clear();
        lastWrite.put(key, Instant.now());

        amazonS3.putObject(bucketName, buildId + "/" + key + "/" + nextId + ".log", output);

        lastId.put(key, nextId);
    }

    private String getFileOutput(String key) {
        StringBuilder sb = new StringBuilder();
        if (outputLines.get(key) == null) {
            return null;
        }
        for (String l : outputLines.get(key)) {
            sb.append(l);
            sb.append("\n");
        }
        return sb.toString();
    }

    private String getLine(OutputType type, String line) {
        switch (type) {
            case INFO:
                return getInfoText(line);
            case ERROR:
                return getErrorText(line);
            default:
                throw new RuntimeException("The output type \"" + type + "\" is unknown");
        }
    }

    private String getInfoText(String line) {
        return "INFO|" + Instant.now().toEpochMilli() + "|" + line;
    }

    private String getErrorText(String line) {
        return "ERROR|" + Instant.now().toEpochMilli() + "|" + line;
    }
}
