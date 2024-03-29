package build.archipelago.packageservice.client;

import build.archipelago.common.*;
import build.archipelago.common.git.models.exceptions.BranchNotFoundException;
import build.archipelago.common.git.models.exceptions.GitDetailsNotFound;
import build.archipelago.common.git.models.exceptions.RepoNotFoundException;
import build.archipelago.common.rest.models.errors.ProblemDetailRestResponse;
import build.archipelago.packageservice.exceptions.*;
import com.google.common.base.Strings;

import java.util.*;

public class PackageExceptionHandler {
    public static final String TYPE_PACKAGE_NOT_FOUND = "package/notFound";
    public static final String TYPE_MULTIPLE_PACKAGE_NOT_FOUND = "package/multipleNotFound";
    public static final String TYPE_PACKAGE_EXISTS = "package/exists";
    public static final String TYPE_GIT_REPOT_NOT_FOUND = "git/repoNotFound";
    public static final String TYPE_GIT_BRANCH_NOT_FOUND = "git/branchNotFound";
    public static final String TYPE_GIT_DETAILS_NOT_FOUND = "git/detailsNotFound";


    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(PackageNotFoundException exp) {
        ProblemDetailRestResponse.ProblemDetailRestResponseBuilder builder = ProblemDetailRestResponse.builder();
        if (exp.getPackages() != null && exp.getPackages().size() > 0) {
            Map<String, Object> packages = new HashMap<>();
            exp.getPackages().forEach(p -> {
                packages.put(p.getName(), p.getVersion());
            });
            builder.error(TYPE_MULTIPLE_PACKAGE_NOT_FOUND)
                    .title("Multiple packages was not found")
                    .status(404)
                    .detail(exp.getMessage())
                    .data(packages);
        } else {
            builder.error(TYPE_PACKAGE_NOT_FOUND)
                    .title("Package was not found")
                    .status(404)
                    .detail(exp.getMessage())
                    .data(new HashMap<>() {{
                        if (exp.getPackageName() != null) {
                            this.put("packageName", exp.getPackageName());
                        }
                        if (exp.getVersion() != null) {
                            this.put("version", exp.getVersion());
                        }
                        if (exp.getHash() != null) {
                            this.put("hash", exp.getHash());
                        }
                    }});
        }
        return builder;
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(PackageExistsException exp) {
        return ProblemDetailRestResponse.builder()
                .error(TYPE_PACKAGE_EXISTS)
                .title("The package exists")
                .status(409)
                .detail(exp.getMessage())
                .data(new HashMap<>() {{
                    if (exp.getPackageName() != null) {
                        this.put("packageName", exp.getPackageName());
                    }
                    if (exp.getVersion() != null) {
                        this.put("version", exp.getVersion());
                    }
                    if (exp.getHash() != null) {
                        this.put("hash", exp.getHash());
                    }
                }});
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(RepoNotFoundException ex) {
        return ProblemDetailRestResponse.builder()
                .error(TYPE_GIT_REPOT_NOT_FOUND)
                .title("The git repository was not found, it might have been deleted")
                .status(400)
                .detail(ex.getMessage())
                .data(new HashMap<>() {{
                    if (ex.getRepo() != null) {
                        this.put("repo", ex.getRepo());
                    }
                }});
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(BranchNotFoundException ex) {
        return ProblemDetailRestResponse.builder()
                .error(TYPE_GIT_BRANCH_NOT_FOUND)
                .title("The git branch was not found, it might have been deleted")
                .status(400)
                .detail(ex.getMessage())
                .data(new HashMap<>() {{
                    if (ex.getBranch() != null) {
                        this.put("branch", ex.getBranch());
                    }
                }});
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(GitDetailsNotFound ex) {
        return ProblemDetailRestResponse.builder()
                .error(TYPE_GIT_DETAILS_NOT_FOUND)
                .title("No git details was found for the account")
                .status(400)
                .detail(ex.getMessage());
    }

    public static Exception createException(ProblemDetailRestResponse problem) {
        switch (problem.getError()) {
            case TYPE_PACKAGE_NOT_FOUND:
                if (problem.getData().containsKey("hash") &&
                        !Strings.isNullOrEmpty((String) problem.getData().get("hash"))) {
                    return new PackageNotFoundException(new ArchipelagoBuiltPackage(
                            (String) problem.getData().get("packageName"),
                            (String) problem.getData().get("version"),
                            (String) problem.getData().get("hash")));
                }
                if (problem.getData().containsKey("version") &&
                        !Strings.isNullOrEmpty((String) problem.getData().get("version"))) {
                    return new PackageNotFoundException(new ArchipelagoPackage(
                            (String) problem.getData().get("packageName"),
                            (String) problem.getData().get("version")));
                }
                return new PackageNotFoundException((String) problem.getData().get("packageName"));
            case TYPE_PACKAGE_EXISTS:
                if (problem.getData().containsKey("hash") &&
                        !Strings.isNullOrEmpty((String) problem.getData().get("hash"))) {
                    return new PackageExistsException(new ArchipelagoBuiltPackage(
                            (String) problem.getData().get("packageName"),
                            (String) problem.getData().get("version"),
                            (String) problem.getData().get("hash")));
                }
                if (problem.getData().containsKey("version")) {
                    return new PackageExistsException(new ArchipelagoPackage(
                            (String) problem.getData().get("packageName"),
                            (String) problem.getData().get("version")));
                }
                return new PackageExistsException((String) problem.getData().get("packageName"));
            case TYPE_GIT_REPOT_NOT_FOUND:
                return new RepoNotFoundException((String) problem.getData().get("repo"));
            case TYPE_GIT_BRANCH_NOT_FOUND:
                return new BranchNotFoundException((String) problem.getData().get("branch"));
            case TYPE_GIT_DETAILS_NOT_FOUND:
                return new GitDetailsNotFound();
            default:
                throw new RuntimeException(problem.getError() + " was not a known package error");
        }
    }
}
