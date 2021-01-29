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
            for (ArchipelagoPackage pkg : directOrAffectedPackages.keySet()) {
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
                vsPackages.put(vsPackage, config.getAllDependencies());
            }
            for (ArchipelagoPackage buildPackage : buildPackages) {
                if (!vsPackages.containsKey(buildPackage)) {
                    BuildConfig config = wsContext.getConfig(buildPackage);
                    vsPackages.put(buildPackage, config.getAllDependencies());
                }
            }
        } catch (Exception e) {
            log.error("Failed to map packages in teh version-set", e);
            throw new RuntimeException(e);
        }
        return vsPackages;
    }
}
