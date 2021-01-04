package build.archipelago.buildserver.builder.git;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

@Slf4j
public class GitWrapper {

    public boolean checkoutBranch(Path path, String branch) {
        try {
            return executeProcess(path, "git", "checkout", branch);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public boolean checkoutCommit(Path path, String commit) {
        // Same as branch, i just like to have it explicit in the usage
        return checkoutBranch(path, commit);
    }

    private boolean executeProcess(Path dir, String... args) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        processBuilder.directory(dir.toFile());
        processBuilder.command(args);
        log.debug("Running command in \"{}\": {}", dir, Strings.join(Arrays.asList(args), ' '));
        try {
            return processBuilder.start().waitFor() == 0;
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }
}
