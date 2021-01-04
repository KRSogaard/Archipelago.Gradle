package build.archipelago.maui.common.cache;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.common.WorkspaceConstants;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class LocalPackageCacher implements PackageCacher {

    private Path cachePath;
    private Path tempFolder;
    private Instant cacheListLastUpdated;
    private PackageCacheList cacheList;
    private HarborClient harborClient;

    public LocalPackageCacher(Path cachePath,
                              Path tempFolder,
                              HarborClient harborClient) throws IOException {
        verifyDir(cachePath);
        verifyDir(tempFolder);
        this.tempFolder = tempFolder;
        this.cachePath = cachePath;
        this.harborClient = harborClient;
    }

    private void verifyDir(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        }
        if (!Files.isDirectory(path)) {
            throw new IOException("Cache path \"" + path + "\" was not a directory");
        }
    }

    @Override
    public PackageCacheList getCurrentCachedPackages() {
        try {
            if (cacheList == null || Files.getLastModifiedTime(cachePath).toInstant().isAfter(cacheListLastUpdated)) {
                generateNewCacheList();
            }
        } catch (IOException e) {
            log.error("Error while generating list of cached packages", e);
        }
        return cacheList;
    }

    private void generateNewCacheList() throws IOException {
        List<ArchipelagoBuiltPackage> packageList = new ArrayList<>();
        cacheListLastUpdated = Files.getLastModifiedTime(cachePath).toInstant();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cachePath)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    packageList.add(ArchipelagoBuiltPackage.parse(path.getFileName().toString()));
                }
            }
        }

        cacheList = new MapPackageCacheList(packageList);
    }

    @Override
    public void cache(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException {
        log.trace("Request to cache package {}", pkg.toString());
        try {
            Path dest = cachePath.resolve(pkg.getBuiltPackageName());
            if (Files.exists(dest)) {
                log.trace("The package {} was already cached", pkg);
                return;
            } else {
                Files.createDirectory(dest);
            }
            Path buildDest = dest.resolve("build");
            if (Files.exists(buildDest)) {
                log.trace("The package {} was already cached", pkg);
                return;
            } else {
                Files.createDirectory(buildDest);
            }

            Path file = harborClient.getBuildArtifact(pkg, tempFolder);
            try {
                new ZipFile(file.toRealPath().toString()).extractAll(buildDest.toRealPath().toString());
            } finally {
                Files.delete(file);
            }

            Files.writeString(dest.resolve(WorkspaceConstants.BUILD_FILE_NAME), harborClient.getConfig(pkg));
            Files.setLastModifiedTime(cachePath, FileTime.from(Instant.now()));
        } catch (IOException exp) {
            throw new RuntimeException(exp);
        }
    }

    @Override
    public Path getCachePath(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException {
        Path pkgRoot = cachePath.resolve(pkg.getBuiltPackageName());
        if (!Files.exists(pkgRoot)) {
            throw new PackageNotFoundException(pkg);
        }
        return pkgRoot;
    }
}
