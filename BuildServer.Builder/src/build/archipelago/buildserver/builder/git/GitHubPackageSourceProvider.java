package build.archipelago.buildserver.builder.git;

import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.maui.common.PackageSourceProvider;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.*;
import org.apache.http.auth.*;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class GitHubPackageSourceProvider implements PackageSourceProvider {

    private String account;
    private String token;

    protected HttpClient client;

    public GitHubPackageSourceProvider(String account,String token) {
        this.account = account;
        this.token = token;

        client = HttpClient
                .newBuilder()
                .build();
    }

    @Override
    public boolean checkOutSource(Path workspaceRoot, String packageName) {
        return checkOutSource(workspaceRoot, packageName, "master");
    }

    @Override
    public boolean checkOutSource(Path workspaceRoot, String packageName, String commit) {
        Path filePath = workspaceRoot.resolve(packageName + "-" + commit + ".zip");

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
