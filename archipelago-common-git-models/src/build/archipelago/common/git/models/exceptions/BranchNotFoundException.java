package build.archipelago.common.git.models.exceptions;

import lombok.Getter;

@Getter
public class BranchNotFoundException extends Exception {
    private String branch;

    public BranchNotFoundException(String branch) {
        super();
        this.branch = branch;
    }
}
