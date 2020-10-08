package build.archipelago.maui.commands;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.GuiceFactory;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.core.workspace.cache.PackageCacher;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.workspace.path.*;
import build.archipelago.maui.core.workspace.path.graph.*;
import build.archipelago.maui.core.workspace.path.recipies.PackageRecipe;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jgrapht.event.*;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.*;
import picocli.CommandLine;

import java.util.*;

@Slf4j
@CommandLine.Command(name = "recursive", aliases = {"rec"}, mixinStandardHelpOptions = true, description = "Recursive commands")
public class RecursiveCommand extends BaseCommand {

    private VersionServiceClient vsClient;
    private PackageCacher packageCacher;
    private MauiPath mauiPath;

    @CommandLine.Parameters(index = "0..*")
    private String[] args;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec commandSpec;

    public RecursiveCommand(MauiPath mauiPath,
                            WorkspaceContextFactory workspaceContextFactory,
                            SystemPathProvider systemPathProvider) {
        super(workspaceContextFactory, systemPathProvider);
        this.mauiPath = mauiPath;
    }

    @Override
    public Integer call() throws Exception {
        if (!requireWorkspace()) {
            System.err.println("Was unable to locate the workspace");
            return 1;
        }
        if (!requirePackage()) {
            System.err.println("Was unable to locate the package");
            return 1;
        }

        ArchipelagoDependencyGraph graph = DependencyGraphGenerator.generateGraph(ws, pkg, DependencyTransversalType.BUILD_TOOLS);

        AbstractGraphIterator<ArchipelagoPackage, ArchipelagoPackageEdge> iterator = new DepthFirstIterator<>(graph, pkg);
        OrderTraversalListener pkgOrderListener = new OrderTraversalListener();
        iterator.addTraversalListener(pkgOrderListener);
        // We just need to loop though to visit all packages, the listener will store the order
        while (iterator.hasNext()) {
            iterator.next();
        }

        for (ArchipelagoPackage pkg : pkgOrderListener.getPackageInOrder()) {
            if (ws.getLocalPackages().stream().anyMatch(lp -> lp.equalsIgnoreCase(pkg.getName()))) {
                // TODO: Change the directory
                //commandSpec.commandLine().execute(args);
            }
        }

        return null;
    }

    private static class OrderTraversalListener implements TraversalListener<ArchipelagoPackage, ArchipelagoPackageEdge> {

        List<ArchipelagoPackage> packageOrder;

        public OrderTraversalListener() {
            packageOrder = new ArrayList<>();
        }

        @Override
        public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {

        }

        @Override
        public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {

        }

        @Override
        public void edgeTraversed(EdgeTraversalEvent e) {

        }

        @Override
        public void vertexTraversed(VertexTraversalEvent e) {

        }

        @Override
        public void vertexFinished(VertexTraversalEvent e) {
            packageOrder.add((ArchipelagoPackage) e.getVertex());
        }

        public List<ArchipelagoPackage> getPackageInOrder() {
            return packageOrder;
        }
    }
}
