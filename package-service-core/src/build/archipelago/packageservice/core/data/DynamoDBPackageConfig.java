package build.archipelago.packageservice.core.data;

import lombok.*;

@Data
@Builder
public class DynamoDBPackageConfig {
    private String packagesTableName;
    private String publicPackagesTableName;
    private String packagesVersionsTableName;
    private String packagesBuildsTableName;
    private String packagesBuildsGitTableName;

    @Override
    public String toString() {
        return "DynamoDBPackageConfig{" +
                "packagesTableName='" + packagesTableName + '\'' +
                ", publicPackagesTableName='" + publicPackagesTableName + '\'' +
                ", packagesVersionsTableName='" + packagesVersionsTableName + '\'' +
                ", packagesBuildsTableName='" + packagesBuildsTableName + '\'' +
                ", packagesBuildsGitTableName='" + packagesBuildsGitTableName + '\'' +
                '}';
    }
}