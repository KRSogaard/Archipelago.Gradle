package build.archipelago.maui.core.workspace.path.graph;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.common.versionset.*;
import build.archipelago.maui.core.exceptions.*;
import build.archipelago.maui.core.workspace.ConfigProvider;
import build.archipelago.maui.core.workspace.models.BuildConfig;
import build.archipelago.maui.core.workspace.path.*;
import org.jgrapht.alg.cycle.CycleDetector;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class DependencyGraphGenerator {

    private Map<String, ArchipelagoDependencyGraph> graphCache;
    private Map<String, ArchipelagoBuiltPackage> buildHashMap;
    private VersionSetRevision versionSetRevision;
    private ConfigProvider configProvide;

    public DependencyGraphGenerator(ConfigProvider configProvider,
                                    VersionSetRevision versionSetRevision) {
        this.versionSetRevision = versionSetRevision;
        this.configProvide = configProvider;
        graphCache = new HashMap<>();
    }

    public ArchipelagoDependencyGraph generateGraph(ArchipelagoPackage rootPkg,
                                                                 DependencyTransversalType transversalType)
            throws PackageNotInVersionSetException, PackageNotLocalException, IOException, PackageNotFoundException, PackageDependencyLoopDetectedException, PackageVersionConflictException {
        String graphKey = getGraphHashKey(rootPkg, transversalType);
        if (graphCache.containsKey(graphKey)) {
            return graphCache.get(graphKey);
        }

        // The build map reused for follow up requests
        createBuildHashMap();

        BuildConfig rootBuildConfig = configProvide.getConfig(getBuildPackage(rootPkg));

        Map<String, Map<String, ArchipelagoBuiltPackage>> pkgMap = new HashMap<>();
        for (ArchipelagoBuiltPackage pkg : versionSetRevision.getPackages()) {
            String name = pkg.getName().toLowerCase();
            String version = pkg.getVersion().toLowerCase();
            if (!pkgMap.containsKey(name)) {
                pkgMap.put(name, new HashMap<>());
            }
            pkgMap.get(name).put(version, pkg);
        }

        if (rootBuildConfig.getResolveConflicts() != null) {
            for (ArchipelagoPackage resolve : rootBuildConfig.getResolveConflicts()) {
                String resolveName = resolve.getName().toLowerCase();
                String resolveVersion = resolve.getVersion().toLowerCase();
                if (!pkgMap.containsKey(resolveName) ||
                        !pkgMap.get(resolveName).containsKey(resolveVersion)) {
                    throw new PackageNotInVersionSetException(resolve);
                }
                List<String> keysToRemove = new ArrayList<>();
                for (String key : pkgMap.get(resolveName).keySet()) {
                    if (!key.equalsIgnoreCase(resolveVersion)) {
                        keysToRemove.add(key);
                    }
                }
                for (String key : keysToRemove) {
                    pkgMap.get(resolveName).remove(key);
                }
            }
        }

        Map<String, Boolean> removed = new HashMap<>();
        if (rootBuildConfig.getRemoveDependencies() != null) {
            for (ArchipelagoPackage removePkg : rootBuildConfig.getRemoveDependencies()) {
                String removeName = removePkg.getName().toLowerCase();
                String removeVersion = removePkg.getVersion().toLowerCase();
                if (!pkgMap.containsKey(removeName) &&
                    !pkgMap.get(removeName).containsKey(removeVersion)) {
                    continue;
                }

                pkgMap.get(removeName).remove(removeVersion);
                removed.put(removePkg.getNameVersion().toLowerCase(), true);
                if (pkgMap.get(removeName).size() == 0) {
                    pkgMap.remove(removeName);
                }
            }
        }

        ArchipelagoDependencyGraph graph = new ArchipelagoDependencyGraph(ArchipelagoPackageEdge.class);
        for (String name : pkgMap.keySet()) {
            for (String version : pkgMap.get(name).keySet()) {
                graph.addVertex(pkgMap.get(name).get(version));
            }
        }

        Map<String, Boolean> seenPackages = new HashMap<>();
        Queue<ArchipelagoPackage> packageQueue = new ConcurrentLinkedQueue<>();
        addPackageToGraph(pkgMap, seenPackages, graph, packageQueue, removed,
                rootPkg, rootBuildConfig, transversalType.getDirectDependencyTypes());

        while(!packageQueue.isEmpty()) {
            ArchipelagoPackage pkg = packageQueue.poll();
            BuildConfig buildConfig = configProvide.getConfig(getBuildPackage(pkg));
            addPackageToGraph(pkgMap, seenPackages, graph, packageQueue, removed,
                    pkg, buildConfig, transversalType.getTransitiveDependencyTypes());
        }

        graphCache.put(graphKey, graph);

        verifyGraph(graph);
        return graph;
    }

    private void verifyGraph(ArchipelagoDependencyGraph graph) throws PackageDependencyLoopDetectedException,
            PackageVersionConflictException {
        detectPackageDependencyLoop(graph);
        detectVersionConflict(graph);
    }

    private void detectVersionConflict(ArchipelagoDependencyGraph graph) throws PackageVersionConflictException {
        Map<String, ArchipelagoPackage> seenPackages = new HashMap<>();
        for (ArchipelagoPackage pkg : graph.vertexSet()) {
            String name = pkg.getName().toLowerCase();
            if (seenPackages.containsKey(name)) {
                throw new PackageVersionConflictException(pkg.getName(), seenPackages.get(name).getVersion(), pkg.getVersion());
            }
            seenPackages.put(name, pkg);
        }
    }

    private void detectPackageDependencyLoop(ArchipelagoDependencyGraph graph) throws PackageDependencyLoopDetectedException {
        CycleDetector<ArchipelagoPackage, ArchipelagoPackageEdge> cycleDetector = new CycleDetector<>(graph);
        Set<ArchipelagoPackage> cycleSet = cycleDetector.findCycles();
        if (cycleSet.size() > 0) {
            throw new PackageDependencyLoopDetectedException(cycleSet);
        }
    }

    private void addPackageToGraph(Map<String, Map<String, ArchipelagoBuiltPackage>> pkgMap,
                                   Map<String, Boolean> seenPackages, ArchipelagoDependencyGraph graph,
                                   Queue<ArchipelagoPackage> packageQueue, Map<String, Boolean> removed,
                                   ArchipelagoPackage pkg, BuildConfig buildConfig, List<DependencyType> dependencyTypes)
            throws PackageNotInVersionSetException, PackageDependencyLoopDetectedException {
        if (seenPackages.containsKey(pkg.getNameVersion().toLowerCase())) {
            return;
        }

        List<ArchipelagoPackageEdge> dependencies = getDependencies(buildConfig, dependencyTypes);
        addGraphEdges(pkgMap, graph, removed, pkg, dependencies);
        seenPackages.put(pkg.getNameVersion().toLowerCase(), true);
        packageQueue.addAll(dependencies.stream().map(d -> d.getDependency().getPackage()).collect(Collectors.toList()));
    }

    private void addGraphEdges(Map<String, Map<String, ArchipelagoBuiltPackage>> pkgMap,
                               ArchipelagoDependencyGraph graph, Map<String, Boolean> removed, ArchipelagoPackage pkg,
                               List<ArchipelagoPackageEdge> dependencies)
            throws PackageNotInVersionSetException, PackageDependencyLoopDetectedException {
        for (ArchipelagoPackageEdge d : dependencies) {
            ArchipelagoPackage pkgDependency = d.getDependency().getPackage();
            // checks if the package hsa been removed, if so do not add the dependency graph
            if (removed.containsKey(d.getDependency().getPackage().getNameVersion().toLowerCase())) {
                return;
            }
            // Detect dependencies to packages that is not in the version-set
            if (!pkgMap.containsKey(pkgDependency.getName().toLowerCase())) {
                throw new PackageNotInVersionSetException(pkgDependency);
            }
            if (!pkgMap.get(pkgDependency.getName().toLowerCase()).containsKey(pkgDependency.getVersion().toLowerCase())) {
                throw new PackageNotInVersionSetException(pkgDependency);
            }

            try {
                graph.addEdge(pkg, d.getDependency().getPackage(), d);
            } catch (IllegalArgumentException exp) {
                throw new PackageDependencyLoopDetectedException(pkg);
            }
        }
    }

    private List<ArchipelagoPackageEdge> getDependencies(BuildConfig bc, List<DependencyType> requestedTypes) {
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

    private void createBuildHashMap() {
        if (buildHashMap != null) {
            return;
        }

        buildHashMap = new HashMap<>();
        versionSetRevision.getPackages().forEach(pkg -> buildHashMap.put(pkg.getNameVersion().toLowerCase(), pkg));
    }

    private ArchipelagoBuiltPackage getBuildPackage(ArchipelagoPackage pkg) throws PackageNotInVersionSetException {
        String key = pkg.getNameVersion().toLowerCase();
        if (!buildHashMap.containsKey(pkg.getNameVersion().toLowerCase())){
            throw new PackageNotInVersionSetException(pkg);
        }
        return buildHashMap.get(key);
    }

    private String getGraphHashKey(ArchipelagoPackage rootPkg,
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
}
