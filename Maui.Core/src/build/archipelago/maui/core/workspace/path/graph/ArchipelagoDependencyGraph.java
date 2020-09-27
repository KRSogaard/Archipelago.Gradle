package build.archipelago.maui.core.workspace.path.graph;

import build.archipelago.common.ArchipelagoPackage;
import org.jgrapht.graph.DirectedMultigraph;

import java.util.function.Supplier;

public class ArchipelagoDependencyGraph extends DirectedMultigraph<ArchipelagoPackage, ArchipelagoPackageEdge> {
    public ArchipelagoDependencyGraph(Class<? extends ArchipelagoPackageEdge> edgeClass) {
        super(edgeClass);
    }

    public ArchipelagoDependencyGraph(Supplier<ArchipelagoPackage> vertexSupplier, Supplier<ArchipelagoPackageEdge> edgeSupplier, boolean weighted) {
        super(vertexSupplier, edgeSupplier, weighted);
    }
}
