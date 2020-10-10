package build.archipelago.maui.core.workspace.path.graph;

import lombok.Getter;
import org.jgrapht.graph.DefaultEdge;

public class ArchipelagoPackageEdge extends DefaultEdge {
    @Getter
    private Dependency dependency;

    public ArchipelagoPackageEdge(Dependency dependency) {
        this.dependency = dependency;
    }
}
