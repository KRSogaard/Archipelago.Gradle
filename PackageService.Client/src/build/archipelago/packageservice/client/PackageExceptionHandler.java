package build.archipelago.packageservice.client;

import build.archipelago.common.*;
import build.archipelago.common.rest.models.errors.ProblemDetailRestResponse;
import build.archipelago.packageservice.exceptions.*;
import com.google.common.base.Strings;

import java.util.*;

public class PackageExceptionHandler {
    public static final String TYPE_PACKAGE_NOT_FOUND = "package/notFound";
    public static final String TYPE_MULTIPLE_PACKAGE_NOT_FOUND = "package/multipleNotFound";

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(PackageNotFoundException exp) {
        ProblemDetailRestResponse.ProblemDetailRestResponseBuilder builder = ProblemDetailRestResponse.builder();
        if (exp.getPackages() != null && exp.getPackages().size() > 0) {
            Map<String, Object> packages = new HashMap<>();
            exp.getPackages().forEach(p -> {
                packages.put(p.getName(), p.getVersion());
            });
            builder.type(TYPE_MULTIPLE_PACKAGE_NOT_FOUND)
                    .title("Multiple packages was not found")
                    .status(404)
                    .detail(exp.getMessage())
                    .data(packages);
        } else {
            builder.type(TYPE_PACKAGE_NOT_FOUND)
                    .title("Package was not found")
                    .status(404)
                    .detail(exp.getMessage())
                    .data(Map.of(
                            "packageName", exp.getPackageName(),
                            "version", exp.getVersion(),
                            "hash", exp.getHash()));
        }
        return builder;
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(PackageExistsException exp) {
        return ProblemDetailRestResponse.builder()
                .type("package/exists")
                .title("The package already exists")
                .status(409)
                .detail(exp.getMessage())
                .data(Map.of(
                        "packageName", exp.getPackageName(),
                        "version", exp.getVersion(),
                        "hash", exp.getHash()));
    }

    public static Exception createException(ProblemDetailRestResponse problem) {
        switch (problem.getType()) {
            case TYPE_PACKAGE_NOT_FOUND:
                if (problem.getData().containsKey("hash") &&
                        !Strings.isNullOrEmpty((String) problem.getData().get("hash"))) {
                    return new PackageNotFoundException(new ArchipelagoBuiltPackage(
                            (String) problem.getData().get("packageName"),
                            (String) problem.getData().get("version"),
                            (String) problem.getData().get("hash")));
                }
                return new PackageNotFoundException(new ArchipelagoPackage(
                        (String) problem.getData().get("packageName"),
                        (String) problem.getData().get("version")));
            default:
                throw new RuntimeException(problem.getType() + " was not a known package error");
        }
    }
}
