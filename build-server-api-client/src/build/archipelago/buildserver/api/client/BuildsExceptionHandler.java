package build.archipelago.buildserver.api.client;

import build.archipelago.buildserver.models.BuildStage;
import build.archipelago.buildserver.models.exceptions.*;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.rest.models.errors.ProblemDetailRestResponse;
import com.google.common.base.Preconditions;

import java.util.HashMap;

public class BuildsExceptionHandler {
    public static final String TYPE_BUILD_NOT_FOUND = "build/notFound";
    public static final String TYPE_STAGE_LOG_NOT_FOUND = "build/stageLogNotFound";
    public static final String TYPE_PACKAGE_LOG_NOT_FOUND = "build/packageLogNotFound";

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(BuildNotFoundException exp) {
        Preconditions.checkNotNull(exp);
        return ProblemDetailRestResponse.builder()
                .type(TYPE_BUILD_NOT_FOUND)
                .title("Build was not found")
                .status(404)
                .detail(exp.getMessage())
                .data(new HashMap<>() {{
                    if (exp.getBuildId() != null) {
                        this.put("buildId", exp.getBuildId());
                    }
                }});
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(StageLogNotFoundException exp) {
        Preconditions.checkNotNull(exp);
        return ProblemDetailRestResponse.builder()
                .type(TYPE_STAGE_LOG_NOT_FOUND)
                .title("Build log was not found")
                .status(404)
                .detail(exp.getMessage())
                .data(new HashMap<>() {{
                    if (exp.getBuildId() != null) {
                        this.put("buildId", exp.getBuildId());
                    }
                    if (exp.getStage() != null) {
                        this.put("stage", exp.getStage());
                    }
                }});
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(PackageLogNotFoundException exp) {
        Preconditions.checkNotNull(exp);
        return ProblemDetailRestResponse.builder()
                .type(TYPE_PACKAGE_LOG_NOT_FOUND)
                .title("Package build log was not found")
                .status(404)
                .detail(exp.getMessage())
                .data(new HashMap<>() {{
                    if (exp.getBuildId() != null) {
                        this.put("buildId", exp.getBuildId());
                    }
                    if (exp.getPkg() != null) {
                        this.put("pkg", exp.getPkg().getNameVersion());
                    }
                }});
    }

    public static Exception createException(ProblemDetailRestResponse problem) {
        Preconditions.checkNotNull(problem);
        switch (problem.getType()) {
            case TYPE_BUILD_NOT_FOUND:
                return new BuildNotFoundException(
                        (String) problem.getData().get("buildId"));
            case TYPE_STAGE_LOG_NOT_FOUND:
                return new StageLogNotFoundException(
                        (String) problem.getData().get("buildId"),
                        BuildStage.getEnum((String) problem.getData().get("stage"))
                );
            case TYPE_PACKAGE_LOG_NOT_FOUND:
                return new PackageLogNotFoundException(
                        (String) problem.getData().get("buildId"),
                        ArchipelagoPackage.parse((String) problem.getData().get("pkg")));
            default:
                throw new RuntimeException(problem.getType() + " was not a known version set error");
        }
    }
}
