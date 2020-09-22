package build.archipelago.maui.configuration;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.versionset.*;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import build.archipelago.versionsetservice.client.model.CreateVersionSetRequest;
import org.springframework.context.annotation.*;

import java.util.List;

@Configuration
public class ServiceConfiguration {

    @Bean
    public VersionServiceClient versionServiceClient() {
        return new VersionServiceClient() {
            @Override
            public void createVersionSet(CreateVersionSetRequest request) throws VersionSetExistsException, VersionSetDoseNotExistsException, PackageNotFoundException {

            }

            @Override
            public String createVersionRevision(String versionSetName, List<ArchipelagoBuiltPackage> packages) throws VersionSetDoseNotExistsException, MissingTargetPackageException, PackageNotFoundException {
                return null;
            }

            @Override
            public VersionSet getVersionSet(String versionSetName) throws VersionSetDoseNotExistsException {
                return VersionSet.builder()
                        .name(versionSetName)
                        .build();
            }

            @Override
            public VersionSetRevision getVersionSetPackages(String versionSetName, String revisionId) throws VersionSetDoseNotExistsException {
                return null;
            }
        };
    }
}
