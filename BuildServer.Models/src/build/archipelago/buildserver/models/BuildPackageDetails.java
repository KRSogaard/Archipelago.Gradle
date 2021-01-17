package build.archipelago.buildserver.models;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class BuildPackageDetails {
    private String packageName;
    private String commit;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(packageName);
        sb.append(";");
        sb.append(commit);
        return sb.toString();
    }

    public static BuildPackageDetails parse(String value) {
        String[] parts = value.split(";");
        if (parts.length != 2) {
            throw new IllegalArgumentException(value + " was not a valid package commit format");
        }
        return BuildPackageDetails.builder()
                .packageName(parts[0])
                .commit(parts[1])
                .build();
    }
}
