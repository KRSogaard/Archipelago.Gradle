package build.archipelago.maui.core.workspace.cache;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.GetPackageBuildResponse;
import buils.archipelago.maui.serializer.WorkspaceConstants;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.*;

@Slf4j
public class LocalPackageCacher implements PackageCacher {

    private Path cachePath;
    private Path tempFolder;
    private Instant cacheListLastUpdated;
    private PackageCacheList cacheList;
    private PackageServiceClient packageClient;

    public LocalPackageCacher(Path cachePath,
                              Path tempFolder,
                              PackageServiceClient packageServiceClient) throws IOException {
        verifyDir(cachePath);
        verifyDir(tempFolder);
        this.tempFolder = tempFolder;
        this.cachePath = cachePath;
        this.packageClient = packageServiceClient;
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
    public void cache(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException, IOException {
        log.trace("Request to cache package {}", pkg.toString());
        Path file = packageClient.getBuildArtifact(pkg, tempFolder);
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

        new ZipFile(file.toRealPath().toString()).extractAll(buildDest.toRealPath().toString());
        GetPackageBuildResponse pkgDetails = packageClient.getPackageBuild(pkg);
        Files.writeString(dest.resolve(WorkspaceConstants.BUILD_FILE_NAME), pkgDetails.getConfig());
        Files.setLastModifiedTime(cachePath, FileTime.from(Instant.now()));
    }
}
