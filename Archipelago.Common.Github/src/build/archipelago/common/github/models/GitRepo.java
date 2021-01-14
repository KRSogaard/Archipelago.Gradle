package build.archipelago.common.github.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitRepo {
    private String name;
    private String url;
    private String cloneUrl;
}