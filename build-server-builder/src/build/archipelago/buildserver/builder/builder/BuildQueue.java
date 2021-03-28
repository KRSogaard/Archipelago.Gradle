package build.archipelago.buildserver.builder.builder;

import build.archipelago.buildserver.builder.builder.helpers.MapHelper;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.common.contexts.WorkspaceContext;
import build.archipelago.maui.common.models.BuildConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Slf4j
public class BuildQueue {
    private Queue<ArchipelagoPackage> queue;
    private Map<ArchipelagoPackage, List<ArchipelagoPackage>> buildDependicies;
    private Map<ArchipelagoPackage, Boolean> doneBuilding;

    public BuildQueue(WorkspaceContext wsContext) {
        queue = new ConcurrentLinkedDeque<>();
        buildDependicies = new HashMap<>();
        doneBuilding = new HashMap<>();

        try {
            Map<ArchipelagoPackage, Boolean> lookupMap = MapHelper.createLookUpMap(wsContext.getLocalArchipelagoPackages());
            for (ArchipelagoPackage pkg : wsContext.getLocalArchipelagoPackages()) {
                BuildConfig config = wsContext.getConfig(pkg);
                List<ArchipelagoPackage> dependencies = config.getAllDependencies().stream()
                        .filter(lookupMap::containsKey).collect(Collectors.toList());
                buildDependicies.put(pkg, dependencies);
                queue.add(pkg);
            }
        } catch (Exception e) {
            log.error("Failed to create build queue with exception: ", e);
            throw new RuntimeException(e);
        }
    }

    public synchronized ArchipelagoPackage getNext() {
        // TODO: Detect where a dependency is not satisfied and never will
        while (queue.size() > 0) {
            ArchipelagoPackage pkg = queue.poll();
            if (pkg == null) {
                return null;
            }
            if (this.isAllDependenciesBuilt(pkg)) {
                return pkg;
            } else {
                queue.add(pkg);
            }
        }
        return null;
    }

    private boolean isAllDependenciesBuilt(ArchipelagoPackage pkg) {
        List<ArchipelagoPackage> dependencies = buildDependicies.get(pkg);
        for (ArchipelagoPackage d : dependencies) {
            if (!doneBuilding.containsKey(d)) {
                return false;
            }
        }
        return true;
    }

    public void setPackageBuilt(ArchipelagoPackage pkg) {
        doneBuilding.put(pkg, true);
    }
}
