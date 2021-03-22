package build.archipelago.packageservice.models;

import com.google.common.collect.ImmutableList;
import lombok.*;

import java.time.Instant;

@Value
@Builder
public class PackageDetails {
    private String name;
    private String owner;
    private Boolean publicPackage;
    private String description;
    private String gitCloneUrl;
    private String gitUrl;
    private String gitRepoName;
    private String gitRepoFullName;
    private Instant created;
    private ImmutableList<PackageDetailsVersion> versions;
}
