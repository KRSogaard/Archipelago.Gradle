package build.archipelago.packageservice.models;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class GitRepoBranchesResponse {
    private List<String> branches;
}
