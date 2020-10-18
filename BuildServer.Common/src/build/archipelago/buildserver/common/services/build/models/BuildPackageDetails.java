package build.archipelago.buildserver.common.services.build.models;

import lombok.*;

@Builder
@Value
public class BuildPackageDetails {
    private String packageName;
    private String branch;
    private String commit;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(packageName);
        sb.append(";");
        sb.append(branch);
        sb.append(";");
        sb.append(commit);
        return sb.toString();
    }

    public static BuildPackageDetails parse(String value) {
        String[] parts = value.split(";");
        if (parts.length != 3) {
            throw new IllegalArgumentException(value + " was not valid");
        }
        return BuildPackageDetails.builder()
                .packageName(parts[0])
                .branch(parts[1])
                .commit(parts[2])
                .build();
    }
}
