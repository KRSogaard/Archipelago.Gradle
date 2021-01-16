package build.archipelago.buildserver.builder.git;

import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.github.GitService;
import build.archipelago.maui.common.PackageSourceProvider;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GitServiceSourceProvider implements PackageSourceProvider {

    private GitService gitService;

    public GitServiceSourceProvider(GitService gitService) {
        this.gitService = gitService;
    }

    @Override
    public boolean checkOutSource(Path workspaceRoot, String packageName, String checkOutUrl) {
        return checkOutSource(workspaceRoot, packageName, checkOutUrl);
    }

    @Override
    public boolean checkOutSource(Path workspaceRoot, String packageName, String checkOutUrl, String commit) {
        Path filePath = workspaceRoot.resolve(packageName + "-" + commit + ".zip");

//        https://github.com/KRSogaard/Archipelago.Gradle/archive/master.zip
//        https://github.com/KRSogaard/Archipelago.Gradle/archive/15c4623b4bb5df76a32119fd12ab0ee0c9cc447c.zip
//        https://github.com/KRSogaard/Archipelago.Gradle/archive/Account-Refactor.zip
//        https://github.com/KRSogaard/Archipelago.Gradle/archive/5c6efb18b2d92fc73666187378e47e58554b3e45.zip

        HttpResponse<Path> response;
        try {
            try {
                String url = String.format("https://github.com/%s/%s/archive/%s.zip", account, packageName, commit);
                HttpRequest httpRequest = HttpRequest.newBuilder(new URI(url))
                        .header("authorization", "Bearer " + token)
                        .GET().build();
                response = client.send(httpRequest, HttpResponse.BodyHandlers.ofFile(filePath));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (response.statusCode() == 401) {
                throw new UnauthorizedException();
            }
            if (response.statusCode() == 302) {
                Optional<String> location = response.headers().firstValue("location");
                if (location.isPresent()) {
                    try {
                        HttpRequest httpRequest = HttpRequest.newBuilder(new URI(location.get()))
                                .header("authorization", "Bearer " + token)
                                .GET().build();
                        response = client.send(httpRequest, HttpResponse.BodyHandlers.ofFile(filePath));

                        if (response.statusCode() != 200) {
                            throw new RuntimeException("Was unable to get the redirected file from github: " + location.get());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw new RuntimeException("Returned 302 but no location was given");
                }
            }
            if (!Files.exists(filePath)) {
                throw new RuntimeException("Failed to download the zip file for package: " + packageName);
            }

            Path packagePath = workspaceRoot.resolve(packageName);
            ZipFile zip = new ZipFile(filePath.toFile());
            zip.extractAll(packagePath.toAbsolutePath().toString());
            // We need to move all files up one dir
            try {
                List<Path> files = Files.list(packagePath).collect(Collectors.toList());
                if (files.size() != 1) {
                    throw new RuntimeException("Github zip was not correctly formatted");
                }

                if (Files.exists(files.get(0)) && Files.isDirectory(files.get(0))) {
                    for (Path file : Files.list(files.get(0)).collect(Collectors.toList())) {
                        Files.move(file, packagePath.resolve(file.getFileName()));
                    }
                    Files.delete(files.get(0));
                } else  {
                    throw new RuntimeException("Github zip was not correctly formatted");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ZipException e) {
            throw new RuntimeException(e);
        } finally {
            if (Files.exists(filePath)) {
                try {
                    Files.delete(filePath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }
}
