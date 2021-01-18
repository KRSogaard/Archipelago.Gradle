package build.archipelago.common.github.models;

import lombok.*;

@Data
@Builder
public class GitRepo {
    private String name;
    private String fullName;
    private String url;
    private String cloneUrl;
    private boolean privateRepo;
}
