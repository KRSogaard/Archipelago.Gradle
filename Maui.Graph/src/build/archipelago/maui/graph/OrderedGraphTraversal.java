package build.archipelago.maui.graph;

import build.archipelago.common.ArchipelagoPackage;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.traverse.AbstractGraphIterator;
import org.jgrapht.traverse.DepthFirstIterator;

import java.util.ArrayList;
import java.util.List;

public class OrderedGraphTraversal {

    public static List<ArchipelagoPackage> bottomUpTransversal(ArchipelagoDependencyGraph graph, ArchipelagoPackage packageRoot) {
        AbstractGraphIterator<ArchipelagoPackage, ArchipelagoPackageEdge> iterator = new DepthFirstIterator<>(graph, packageRoot);
        OrderTraversalListener pkgOrderListener = new OrderTraversalListener();
        iterator.addTraversalListener(pkgOrderListener);
        // We just need to loop though to visit all packages, the listener will store the order
        while (iterator.hasNext()) {
            iterator.next();
        }
        return pkgOrderListener.getPackageInOrder();
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
