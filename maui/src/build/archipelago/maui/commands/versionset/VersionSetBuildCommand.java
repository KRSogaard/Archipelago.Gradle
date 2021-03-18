package build.archipelago.maui.commands.versionset;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.common.contexts.WorkspaceContext;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.actions.BaseAction;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.graph.ArchipelagoDependencyGraph;
import build.archipelago.maui.graph.DependencyGraphGenerator;
import build.archipelago.maui.graph.DependencyTransversalType;
import build.archipelago.maui.path.MauiPath;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.jgrapht.traverse.DepthFirstIterator;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Slf4j
@CommandLine.Command(name = "build", mixinStandardHelpOptions = true, description = "")
public class VersionSetBuildCommand extends BaseAction implements Callable<Integer> {

    @CommandLine.Option(names = { "-vs", "--versionset"}, required = true)
    private String versionSetName;

    @CommandLine.Option(names = { "-r", "--revision"}, required = true)
    private String revisionId;

    @CommandLine.Option(names = { "-o", "--out"}, required = true)
    private String outDir;

    private HarborClient harborClient;
    private OutputWrapper out;
    private DependencyGraphGenerator dependencyGraphGenerator;

    public VersionSetBuildCommand(WorkspaceContextFactory workspaceContextFactory,
                                  SystemPathProvider systemPathProvider,
                                  DependencyGraphGenerator dependencyGraphGenerator,
                                  OutputWrapper out,
                                  HarborClient harborClient) {
        super(workspaceContextFactory, systemPathProvider, out);
        this.harborClient = harborClient;
        this.out = out;
        this.dependencyGraphGenerator = dependencyGraphGenerator;
    }

    @Override
    public Integer call() throws Exception {
        Path outputDir = Paths.get(outDir);
        if (!Files.exists(outputDir)) {
            try {
            Files.createDirectories(outputDir);
            } catch (IOException exp) {
                out.error("Was unable to the directory '%s'", outputDir);
                return 1;
            }
        } else {
            if (Files.list(outputDir).findFirst().isPresent()) {
                out.error("The output directory was not empty '%s'", outputDir);
                return 1;
            }
        }

        try {
            VersionSet versionSet = harborClient.getVersionSet(versionSetName);
            VersionSetRevision versionSetRevision = harborClient.getVersionSetRevision(versionSetName, revisionId);
            Map<String, ArchipelagoBuiltPackage> builtPackageMap = new HashMap<>();

            for (ArchipelagoBuiltPackage pkg : versionSetRevision.getPackages()) {
                builtPackageMap.put(pkg.getNameVersion(), pkg);
            }

            // TODO Use this later
//            WorkspaceContext context = workspaceContextFactory.create(outputDir);
//            context.setVersionSet(versionSet.getName());
//
//            ArchipelagoDependencyGraph graph = dependencyGraphGenerator.generateGraph(
//                    context, versionSet.getTarget(), DependencyTransversalType.RUNTIME);

            //ImmutableList<ArchipelagoPackage> packages = ImmutableList.copyOf(new DepthFirstIterator<>(graph, versionSet.getTarget()));
            List<ArchipelagoBuiltPackage> packages = versionSetRevision.getPackages();

            for (ArchipelagoPackage pkg : packages) {
                ArchipelagoBuiltPackage build = builtPackageMap.get(pkg.getNameVersion());
                if (build == null) {
                    out.error("The package \"" + pkg.getNameVersion() + "\" was not found locally");
                    return 1;
                }
                out.write("Downloading build artifact for " + build.getBuiltPackageName());
                Path zipFile = harborClient.getBuildArtifact(build, outputDir);
                out.write("Download done, extracting " + build.getBuiltPackageName());
                try {
                    ZipFile zip = new ZipFile(zipFile.toFile());
                    zip.extractAll(outputDir.toAbsolutePath().toString());
                } finally {
                    if (Files.exists(zipFile)) {
                        Files.delete(zipFile);
                    }
                }
                out.write("Extracting of " + build.getBuiltPackageName() + " done");
            }
        } catch (VersionSetDoseNotExistsException e) {
            out.error("Unable to find the Version-Set \"%s\"", versionSetName);
            return 1;
        }

        out.write("Version Set has been built");
        return 0;
    }


}
