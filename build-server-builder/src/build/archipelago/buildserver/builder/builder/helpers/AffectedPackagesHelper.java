package build.archipelago.buildserver.builder.builder.helpers;

import build.archipelago.common.*;
import build.archipelago.maui.common.contexts.WorkspaceContext;
import build.archipelago.maui.common.models.BuildConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class AffectedPackagesHelper {

    public static List<ArchipelagoPackage> findAffectedPackages(WorkspaceContext wsContext, List<ArchipelagoPackage> buildPackages) {
        Map<ArchipelagoPackage, List<ArchipelagoPackage>> vsPackages = getVersionSetPackageMap(wsContext, buildPackages);
        Map<ArchipelagoPackage, Boolean> directOrAffectedPackages = MapHelper.createLookUpMap(buildPackages);
        int previousSize;
        do {
            previousSize = directOrAffectedPackages.size();
            for (ArchipelagoPackage pkg : new ArrayList<>(directOrAffectedPackages.keySet())) {
                for (ArchipelagoPackage vsPackage : vsPackages.keySet()) {
                    if (directOrAffectedPackages.containsKey(vsPackage)) {
                        continue;
                    }
                    if (vsPackages.get(vsPackage).stream().anyMatch(dependency -> dependency.equals(pkg))) {
                        directOrAffectedPackages.put(vsPackage, true);
                        break;
                    }
                }
            }
        } while (previousSize != directOrAffectedPackages.size());

        return new ArrayList<>(directOrAffectedPackages.keySet());
    }

    private static Map<ArchipelagoPackage, List<ArchipelagoPackage>> getVersionSetPackageMap(WorkspaceContext wsContext,
                                                                                             List<ArchipelagoPackage> buildPackages) {
        Map<ArchipelagoPackage, List<ArchipelagoPackage>> vsPackages = new HashMap<>();
        try {
            for (ArchipelagoBuiltPackage vsPackage : wsContext.getVersionSetRevision().getPackages()) {
                BuildConfig config = wsContext.getConfig(vsPackage);
                vsPackages.put(vsPackage.asBase(), config.getAllDependencies());
            }
            // Find new packages that was not in the version set before
            for (ArchipelagoPackage buildPackage : buildPackages) {
                ArchipelagoPackage clean = buildPackage.asBase();
                if (!vsPackages.containsKey(clean)) {
                    BuildConfig config = wsContext.getConfig(buildPackage);
                    vsPackages.put(clean, config.getAllDependencies());
                }
            }
        } catch (Exception e) {
            log.error("Failed to map packages in the version-set", e);
            throw new RuntimeException(e);
        }
        return vsPackages;
    }
}
