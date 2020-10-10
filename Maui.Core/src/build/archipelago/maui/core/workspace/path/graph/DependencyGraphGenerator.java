package build.archipelago.maui.core.workspace.path.graph;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.maui.core.exceptions.*;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContext;
import build.archipelago.maui.core.workspace.models.BuildConfig;
import build.archipelago.maui.core.workspace.path.*;
import lombok.Getter;
import org.jgrapht.alg.cycle.CycleDetector;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class DependencyGraphGenerator {

    protected static Map<String, ArchipelagoDependencyGraph> graphCache;

    public static void clearCache() {
        if (graphCache != null) {
            graphCache.clear();
        }
    }

    private static ArchipelagoDependencyGraph getCachedGraph(ArchipelagoPackage rootPkg,
                                                             DependencyTransversalType transversalType) {
        if (graphCache == null) {
            graphCache = new HashMap<>();
            return null;
        }
        String key = getGraphHashKey(rootPkg, transversalType);
        if (graphCache.containsKey(key)) {
            return graphCache.get(key);
        }
        return null;
    }

    public static ArchipelagoDependencyGraph generateGraph(WorkspaceContext workspaceContext,
                                                           ArchipelagoPackage rootPkg,
                                                           DependencyTransversalType transversalType)
            throws PackageNotInVersionSetException, PackageNotLocalException, IOException, PackageNotFoundException,
            PackageDependencyLoopDetectedException, PackageVersionConflictException, VersionSetNotSyncedException, LocalPackageMalformedException {
        ArchipelagoDependencyGraph cacheGraph = getCachedGraph(rootPkg, transversalType);
        if (cacheGraph != null) {
            return cacheGraph;
        }

        GraphGenerationContext context = new GraphGenerationContext(rootPkg, workspaceContext);

        if (context.getRootBuildConfig().getResolveConflicts() != null) {
            for (ArchipelagoPackage resolve : context.getRootBuildConfig().getResolveConflicts()) {
                if (!context.isPackageInVersionSet(resolve)) {
                    throw new PackageNotInVersionSetException(resolve);
                }
            }
        }

        // The dependency vertexes will be added when we see a dependency link to them
        context.getGraph().addVertex(rootPkg);
        addPackageToGraph(context, rootPkg, context.getRootBuildConfig(), transversalType.getDirectDependencyTypes());

        while(!context.getPackageQueue().isEmpty()) {
            ArchipelagoPackage pkg = context.getPackageQueue().poll();
            if (context.isPackageRemoved(pkg)) {
                continue;
            }
            if (pkg == null) {
                break;
            }
            BuildConfig buildConfig = workspaceContext.getConfig(context.getBuildPackage(pkg));
            addPackageToGraph(context, pkg, buildConfig, transversalType.getTransitiveDependencyTypes());
        }

        graphCache.put(getGraphHashKey(rootPkg, transversalType), context.getGraph());

        verifyGraph(context.getGraph());
        return context.getGraph();
    }

    private static void verifyGraph(ArchipelagoDependencyGraph graph) throws PackageDependencyLoopDetectedException,
            PackageVersionConflictException {
        detectPackageDependencyLoop(graph);
        detectVersionConflict(graph);
    }

    private static void detectVersionConflict(ArchipelagoDependencyGraph graph) throws PackageVersionConflictException {
        Map<String, ArchipelagoPackage> seenPackages = new HashMap<>();
        for (ArchipelagoPackage pkg : graph.vertexSet()) {
            String name = pkg.getName().toLowerCase();
            if (seenPackages.containsKey(name)) {
                throw new PackageVersionConflictException(pkg.getName(), seenPackages.get(name).getVersion(), pkg.getVersion());
            }
            seenPackages.put(name, pkg);
        }
    }

    private static void detectPackageDependencyLoop(ArchipelagoDependencyGraph graph) throws PackageDependencyLoopDetectedException {
        CycleDetector<ArchipelagoPackage, ArchipelagoPackageEdge> cycleDetector = new CycleDetector<>(graph);
        Set<ArchipelagoPackage> cycleSet = cycleDetector.findCycles();
        if (cycleSet.size() > 0) {
            throw new PackageDependencyLoopDetectedException(cycleSet);
        }
    }

    private static void addPackageToGraph(GraphGenerationContext context, ArchipelagoPackage pkg, BuildConfig buildConfig,
                                   List<DependencyType> dependencyTypes)
            throws PackageNotInVersionSetException, PackageDependencyLoopDetectedException {
        if (context.hasSeenPackage(pkg)) {
            return;
        }

        List<ArchipelagoPackageEdge> dependencies = getDependencies(buildConfig, dependencyTypes);
        addGraphEdges(context, pkg, dependencies);
        context.seePackage(pkg);
        context.addDependenciesToBeAdded(dependencies);
    }

    private static void addGraphEdges(GraphGenerationContext context, ArchipelagoPackage pkg,
                               List<ArchipelagoPackageEdge> dependencies)
            throws PackageNotInVersionSetException, PackageDependencyLoopDetectedException {
        for (ArchipelagoPackageEdge d : dependencies) {
            ArchipelagoPackage pkgDependency = d.getDependency().getPackage();

            // We have to do conflict resolves first, so the removes can be applied to the resolved package
            ArchipelagoPackage conflictResolve = context.getConflictResolveForPackage(pkgDependency);
            if (conflictResolve != null) {
                pkgDependency = conflictResolve;
                d.getDependency().setPackage(conflictResolve);
            }

            if (context.isPackageRemoved(pkgDependency)) {
                continue;
            }

            if (!context.isPackageInVersionSet(pkgDependency)) {
                throw new PackageNotInVersionSetException(pkgDependency);
            }

            try {
                // This package or dependency may have been added before, that is ok. The graph methods will detect this
                // and ignore the add call
                context.getGraph().addVertex(pkgDependency);
                context.getGraph().addEdge(pkg, d.getDependency().getPackage(), d);
            } catch (IllegalArgumentException exp) {
                if (exp.getMessage().contains("loop")) {
                    throw new PackageDependencyLoopDetectedException(pkg);
                } else {
                    throw exp;
                }
            }
        }
    }

    private static List<ArchipelagoPackageEdge> getDependencies(BuildConfig bc, List<DependencyType> requestedTypes) {
        List<ArchipelagoPackageEdge> packages = new ArrayList<>();
        for (DependencyType type : requestedTypes) {
            switch (type) {
                case LIBRARY:
                    if (bc.getLibraries() != null) {
                        packages.addAll(bc.getLibraries().stream().map(d -> new ArchipelagoPackageEdge(
                                new Dependency(DependencyType.LIBRARY, d))).collect(Collectors.toList()));
                    }
                    break;
                case RUNTIME:
                    if (bc.getRuntime() != null) {
                        packages.addAll(bc.getRuntime().stream().map(d -> new ArchipelagoPackageEdge(
                                new Dependency(DependencyType.RUNTIME, d))).collect(Collectors.toList()));
                    }
                    break;
                case TEST:
                    if (bc.getTest() != null) {
                        packages.addAll(bc.getTest().stream().map(d -> new ArchipelagoPackageEdge(
                                new Dependency(DependencyType.TEST, d))).collect(Collectors.toList()));
                    }
                    break;
                case BUILDTOOLS:
                    if (bc.getBuildTools() != null) {
                        packages.addAll(bc.getBuildTools().stream().map(d -> new ArchipelagoPackageEdge(
                                new Dependency(DependencyType.BUILDTOOLS, d))).collect(Collectors.toList()));
                    }
                    break;
                default:
                    throw new RuntimeException("Unknown Dependency type " + type);
            }
        }
        return packages;
    }

    private static String getGraphHashKey(ArchipelagoPackage rootPkg,
                                   DependencyTransversalType transversalType) {
        StringBuilder builder = new StringBuilder();
        builder.append(rootPkg.getNameVersion().toLowerCase())
                .append("|Direct:");
        for (DependencyType type : transversalType.getDirectDependencyTypes()) {
            builder.append(type.name())
                    .append(",");
        }
        builder.append("|Transitive:");
        for (DependencyType type : transversalType.getTransitiveDependencyTypes()) {
            builder.append(type.name())
                    .append(",");
        }
        return builder.toString();
    }

    private static class GraphGenerationContext {
        @Getter
        private final BuildConfig rootBuildConfig;
        @Getter
        private final Map<String, Boolean> seenPackages;
        @Getter
        private final Queue<ArchipelagoPackage> packageQueue;
        @Getter
        private final ArchipelagoDependencyGraph graph;
        private final WorkspaceContext workspaceContext;
        private final Map<String, ArchipelagoBuiltPackage> buildMap;

        public GraphGenerationContext(ArchipelagoPackage pkg, WorkspaceContext workspaceContext)
                throws IOException, VersionSetNotSyncedException, PackageNotInVersionSetException,
                PackageNotFoundException, PackageNotLocalException, LocalPackageMalformedException {
            this.workspaceContext = workspaceContext;
            buildMap = createBuildHashMap();
            this.rootBuildConfig = workspaceContext.getConfig(pkg);
            seenPackages = new HashMap<>();
            packageQueue = new ConcurrentLinkedQueue<>();
            graph = new ArchipelagoDependencyGraph(ArchipelagoPackageEdge.class);
        }

        public boolean hasSeenPackage(ArchipelagoPackage pkg) {
            return seenPackages.containsKey(pkg.getNameVersion().toLowerCase());
        }
        private void seePackage(ArchipelagoPackage pkg) {
            seenPackages.put(pkg.getNameVersion().toLowerCase(), true);
        }
        private void addDependenciesToBeAdded(List<ArchipelagoPackageEdge> packages) {
            packageQueue.addAll(packages.stream()
                    .map(d -> d.getDependency().getPackage())
                    .filter(d -> !hasSeenPackage(d))
                    .collect(Collectors.toList()));
        }

        private boolean isPackageRemoved(ArchipelagoPackage pkg) {
            if (rootBuildConfig.getRemoveDependencies() == null) {
                return false;
            }
            return rootBuildConfig.getRemoveDependencies().stream().anyMatch(r -> r.equals(pkg));
        }

        public ArchipelagoPackage getConflictResolveForPackage(ArchipelagoPackage pkgDependency) {
            if (rootBuildConfig.getResolveConflicts() == null) {
                return null;
            }
            Optional<ArchipelagoPackage> conflictResolve = rootBuildConfig.getResolveConflicts().stream()
                    .filter(r -> r.getName().equalsIgnoreCase(pkgDependency.getName()))
                    .findFirst();
            if (conflictResolve.isPresent()) {
                if (conflictResolve.equals(pkgDependency)) {
                    return null;
                }
                return conflictResolve.get();
            }
            return null;
        }

        public boolean isPackageInVersionSet(ArchipelagoPackage pkg) {
            return buildMap.containsKey(pkg.getNameVersion().toLowerCase());
        }

        public ArchipelagoBuiltPackage getBuildPackage(ArchipelagoPackage pkg) throws PackageNotInVersionSetException {
            String key = pkg.getNameVersion().toLowerCase();
            if (!buildMap.containsKey(pkg.getNameVersion().toLowerCase())){
                throw new PackageNotInVersionSetException(pkg);
            }
            return buildMap.get(key);
        }

        private Map<String, ArchipelagoBuiltPackage> createBuildHashMap()
                throws IOException, VersionSetNotSyncedException {
            Map<String, ArchipelagoBuiltPackage> buildHashMap = new HashMap<>();
            workspaceContext.getVersionSetRevision().getPackages().forEach(pkg -> buildHashMap.put(pkg.getNameVersion().toLowerCase(), pkg));
            for (ArchipelagoPackage pkg : workspaceContext.getLocalArchipelagoPackages()) {
                if (!buildHashMap.containsKey(pkg.getNameVersion().toLowerCase())) {
                    buildHashMap.put(pkg.getNameVersion().toLowerCase(), new ArchipelagoBuiltPackage(pkg, "local"));
                }
            }
            return buildHashMap;
        }
    }
}
