package build.archipelago.buildserver.builder;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Slf4j
public class MauiWrapper {
    public String mauiPath;
    public Path outputFilesPath;

    public MauiWrapper(String mauiPath, Path outputFilesPath) {
        this.mauiPath = mauiPath;
        this.outputFilesPath = outputFilesPath;
    }

    public int execute(Path executionFolder, String... args) throws IOException {
        return execute(executionFolder, false, false, args).getExitCode();
    }
    public int executeWithWorkspaceCache(Path executionFolder, String... args) throws IOException {
        return execute(executionFolder, true, false, args).getExitCode();
    }
    public ExecutionResult executeWithOutput(Path executionFolder, String... args) throws IOException {
        return execute(executionFolder, false, true, args);
    }
    public ExecutionResult executeWithWorkspaceCacheWithOutput(Path executionFolder, String... args) throws IOException {
        return execute(executionFolder, true, true, args);
    }

    private ExecutionResult execute(Path executionFolder, boolean useWorkspaceCache, boolean storeOutput, String... args) throws IOException {
        String uuid = UUID.randomUUID().toString();
        Path outputFile = outputFilesPath.resolve(uuid + ".output.log");
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(executionFolder.toFile());
        if (storeOutput) {
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(outputFile.toFile());
        } else {
            outputFile = null;
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        }
        if (useWorkspaceCache) {
            processBuilder.environment().put("MAUI_USE_WORKSPACE_CACHE", "true");
        }
        processBuilder.command(createMauiCommand(args));

        try {
            int exitCode = processBuilder.start().waitFor();
            return new ExecutionResult(exitCode, outputFile);
        } catch (Exception e) {
            log.warn("Process thread was interrupted.");
            try {
                if (Files.exists(outputFile)) {
                    Files.delete(outputFile);
                }
            } catch (IOException e2) {
                log.error("Failed to delete the log files after an exception", e2);
            }
            throw new RuntimeException(e);
        }
    }

    private List<String> createMauiCommand(String... args) {
        List<String> cmds = new ArrayList<>();
        cmds.add(mauiPath);
        for (String arg : args) {
            cmds.add(arg);
        }
        return cmds;
    }

    public boolean verifyMauiIsPreset() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(mauiPath, "version");
            int versionExit = processBuilder.start().waitFor();
            return versionExit == 0;
        } catch (Exception e) {
            log.error("Failed maui preset check with exception", e);
            return false;
        }
    }

    public static class ExecutionResult {
        private int exitCode;
        private Path outputFile;

        private ExecutionResult(int exitCode, Path outputFile) {
            this.exitCode = exitCode;
            this.outputFile = outputFile;
        }

        public int getExitCode() {
            return exitCode;
        }

        public Path getOutputFile() {
            return outputFile;
        }

        public void clearFiles() {
            try {
                if (Files.exists(outputFile)) {
                    Files.delete(outputFile);
                }
            } catch (IOException e) {
                log.warn("Failed to delete the log file " + outputFile, e);
            }
        }
    }
}
