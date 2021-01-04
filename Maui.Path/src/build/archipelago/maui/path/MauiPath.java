package build.archipelago.maui.path;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.DependencyTransversalTypeNotFoundException;
import build.archipelago.common.exceptions.PackageNotInVersionSetException;
import build.archipelago.common.exceptions.PathStringInvalidException;
import build.archipelago.common.exceptions.RecipeNotFoundException;
import build.archipelago.maui.common.contexts.WorkspaceContext;
import build.archipelago.maui.graph.ArchipelagoDependencyGraph;
import build.archipelago.maui.graph.DependencyGraphGenerator;
import build.archipelago.maui.graph.DependencyTransversalType;
import build.archipelago.maui.path.recipies.Recipe;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.traverse.DepthFirstIterator;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class MauiPath {

    private DependencyGraphGenerator dependencyGraphGenerator;
    private List<Recipe> recipes;

    public MauiPath(List<Recipe> recipes,
                    DependencyGraphGenerator dependencyGraphGenerator) {
        this.dependencyGraphGenerator = dependencyGraphGenerator;
        this.recipes = recipes;
    }

    public ImmutableList<String> getPaths(WorkspaceContext workspaceContext, ArchipelagoPackage targetpackage,
                                          DependencyTransversalType dependencyTransversalType, Class recipe)
            throws Exception {
        StringBuilder pathLine = new StringBuilder();
        pathLine.append(dependencyTransversalType.getName());
        pathLine.append(".");

        boolean found = false;
        for (Recipe r : recipes) {
            if (recipe.equals(r.getClass())) {
                pathLine.append(r.getName());
                found = true;
                break;
            }
        }
        if (!found) {
            throw new RecipeNotFoundException(recipe.getName());
        }


        return getPaths(workspaceContext, targetpackage, pathLine.toString());
    }
    // It returns string instead of Path to allow recipes to be creative wit the usage
    public ImmutableList<String> getPaths(WorkspaceContext workspaceContext, ArchipelagoPackage rootPackage, String pathLine)
            throws Exception {

        ImmutableList.Builder<String> list = ImmutableList.<String>builder();

        for (String request : pathLine.split(";")) {
            log.info("Generating graph for: {}", request);
            PathProperties pathProperties = PathProperties.parse(request);
            ArchipelagoPackage targetPackage = rootPackage;

            if (pathProperties.targetPackage != null) {
                targetPackage = ArchipelagoPackage.parse(pathProperties.targetPackage);
            }

            if (!workspaceContext.isPackageInVersionSet(targetPackage)) {
                throw new PackageNotInVersionSetException(targetPackage);
            }

            Optional<Recipe> optionalRecipe = recipes.stream()
                    .filter(r -> r.getName().equalsIgnoreCase(pathProperties.getRecipe()))
                    .findFirst();
            if (optionalRecipe.isEmpty()) {
                throw new RecipeNotFoundException(pathProperties.getRecipe());
            }
            Recipe recipe = optionalRecipe.get();

            DependencyTransversalType transversalType = getTransversalType(pathProperties.transversalType);
            ArchipelagoDependencyGraph graph = dependencyGraphGenerator.generateGraph(workspaceContext, targetPackage, transversalType);

            Iterator<ArchipelagoPackage> iterator = new DepthFirstIterator<>(graph, targetPackage);
            while (iterator.hasNext()) {
                ArchipelagoPackage pkg = iterator.next();
                if (!transversalType.includeRoot() && targetPackage.equals(pkg)) {
                    continue;
                }
                list.addAll(recipe.execute(pkg, workspaceContext));
            }
        }

        return list.build();
    }

    private DependencyTransversalType getTransversalType(String name) throws DependencyTransversalTypeNotFoundException {
        Optional<DependencyTransversalType> optional = Arrays.stream(DependencyTransversalType.values())
                .filter(d -> d.getName().equalsIgnoreCase(name)).findFirst();
        if (optional.isEmpty()) {
            throw new DependencyTransversalTypeNotFoundException(name);
        }
        return optional.get();
    }

    protected static class PathProperties {
        private static final Pattern re = Pattern.compile("^(\\[([^\\]]+)\\])?([^.\\[\\]]+)\\.(.+)");

        @Getter
        private String targetPackage;
        @Getter
        private String transversalType;
        @Getter
        private String recipe;

        public PathProperties(String targetPackage, String transversalType, String recipe) {
            this.targetPackage = targetPackage;
            this.transversalType = transversalType;
            this.recipe = recipe;
        }

        protected static PathProperties parse(String input) throws PathStringInvalidException {
            Matcher matcher = re.matcher(input);
            while (matcher.find()){
                if (matcher.groupCount() == 4) {
                    return new PathProperties(matcher.group(2), matcher.group(3), matcher.group(4));
                }
            }

            throw new PathStringInvalidException(input);
        }
    }
}
