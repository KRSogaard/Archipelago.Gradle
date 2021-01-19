package build.archipelago.versionsetservice.client;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.rest.models.errors.ProblemDetailRestResponse;
import build.archipelago.versionsetservice.exceptions.*;
import com.google.common.base.Preconditions;

import java.util.HashMap;

public class VersionSetExceptionHandler {

    public static final String TYPE_VERSION_SET_NOT_FOUND = "versionSet/notFound";
    public static final String TYPE_VERSION_SET_EXISTS = "versionSet/exists";
    public static final String TYPE_VERSION_SET_TARGETS_MISSING = "versionSet/targetMissing";

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(VersionSetDoseNotExistsException exp) {
        Preconditions.checkNotNull(exp);
        return ProblemDetailRestResponse.builder()
                .type(TYPE_VERSION_SET_NOT_FOUND)
                .title("Version set was not found")
                .status(404)
                .detail(exp.getMessage())
                .data(new HashMap<>() {{
                    if (exp.getVersionSet() != null) {
                        this.put("versionSet", exp.getVersionSet());
                    }
                    if (exp.getRevision() != null) {
                        this.put("revision", exp.getRevision());
                    }
                }});
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(VersionSetExistsException exp) {
        Preconditions.checkNotNull(exp);
        return ProblemDetailRestResponse.builder()
                .type(TYPE_VERSION_SET_EXISTS)
                .title("Version set already exists")
                .status(409)
                .detail(exp.getMessage())
                .data(new HashMap<>() {{
                    if (exp.getVersionSet() != null) {
                        this.put("versionSet", exp.getVersionSet());
                    }
                }});
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(MissingTargetPackageException exp) {
        Preconditions.checkNotNull(exp);
        return ProblemDetailRestResponse.builder()
                .type(TYPE_VERSION_SET_TARGETS_MISSING)
                .title("A target was not version set")
                .status(400)
                .detail(exp.getMessage())
                .data(new HashMap<>() {{
                    if (exp.getPackageName() != null) {
                        this.put("packageName", exp.getPackageName());
                    }
                    if (exp.getVersion() != null) {
                        this.put("version", exp.getVersion());
                    }
                }});
    }


    public static Exception createException(ProblemDetailRestResponse problem) {
        switch (problem.getType()) {
            case TYPE_VERSION_SET_NOT_FOUND:
                if (problem.getData().containsKey("revision")) {
                    return new VersionSetDoseNotExistsException(
                            (String) problem.getData().get("versionSet"),
                            (String) problem.getData().get("revision"));
                }
                return new VersionSetDoseNotExistsException(
                        (String) problem.getData().get("versionSet"));
            case TYPE_VERSION_SET_EXISTS:
                return new VersionSetExistsException(
                        (String) problem.getData().get("versionSet"));
            case TYPE_VERSION_SET_TARGETS_MISSING:
                return new MissingTargetPackageException(new ArchipelagoPackage(
                        (String) problem.getData().get("packageName"),
                        (String) problem.getData().get("version")));
            default:
                throw new RuntimeException(problem.getType() + " was not a known version set error");
        }
    }
}
