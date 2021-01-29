package build.archipelago.buildserver.builder.builder.helpers;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

@Slf4j
public class PathHelper {
    public static Path createBuildRoot(Path buildLocation, String buildId) {
        Path location = buildLocation.resolve(buildId);
        if (!Files.exists(location)) {
            try {
                Files.createDirectory(location);
            } catch (IOException e) {
                log.error("Failed to create build dir " + location);
                throw new RuntimeException(e);
            }
        }
        return location;
    }


    public static Path findPackageDir(Path workspace, String name) {
        Path packagePath = null;
        try {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(workspace)) {
                for (Path path : stream) {
                    if (Files.isDirectory(path) &&
                            path.getFileName().toString().equalsIgnoreCase(name)) {
                        packagePath = path;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            log.error("Was unable to find the package dir for " + name, e);
            throw new RuntimeException(e);
        }
        return packagePath;
    }

    public static void deleteFolder(Path path) {
        if (path != null && Files.exists(path) && Files.isDirectory(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                log.error("Failed to delete workspace", e);
                throw new RuntimeException(e);
            }
        }
    }
}
