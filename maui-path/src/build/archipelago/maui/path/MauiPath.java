package build.archipelago.maui.path;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.*;
import build.archipelago.maui.common.contexts.WorkspaceContext;
import build.archipelago.maui.graph.*;
import build.archipelago.maui.path.recipies.Recipe;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.traverse.DepthFirstIterator;

import java.util.*;
import java.util.regex.*;

@Slf4j
public class MauiPath {

    private DependencyGraphGenerator dependencyGraphGenerator;
    private List<Recipe> recipes;

    public MauiPath(List<Recipe> recipes,
                    DependencyGraphGenerator dependencyGraphGenerator) {
        this.dependencyGraphGenerator = dependencyGraphGenerator;
        this.recipes = recipes;
    }

//    public void LoadRecipesFromClasspath(String classPath) {
//
//    }

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

            if (pathProperties.targetPackage != null
                    && !rootPackage.getName().equalsIgnoreCase(pathProperties.targetPackage)
                    && !rootPackage.getNameVersion().equalsIgnoreCase(pathProperties.targetPackage)) {
                if (!pathProperties.targetPackage.contains("-")) {
                    Optional<ArchipelagoPackage> pkg = workspaceContext.getVersionSetRevision()
                            .getPackages().stream()
                            .filter(f -> f.getName().equalsIgnoreCase(pathProperties.targetPackage))
                            .map(b -> (ArchipelagoPackage)b)
                            .findFirst();
                    if (pkg.isEmpty()) {
                        pkg = workspaceContext.getLocalArchipelagoPackages().stream()
                                .filter(f -> f.getName().equalsIgnoreCase(pathProperties.targetPackage))
                                .findFirst();
                        if (pkg.isEmpty()) {
                            throw new PackageNotInVersionSetException(pathProperties.targetPackage);
                        }
                    }
                    targetPackage = ArchipelagoPackage.parse(pkg.get().getNameVersion());
                } else {
                    targetPackage = ArchipelagoPackage.parse(pathProperties.targetPackage);
                }
            }

            if (!workspaceContext.isPackageInVersionSetOrLocal(targetPackage)) {
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

            ImmutableList<ArchipelagoPackage> packages = ImmutableList.copyOf(new DepthFirstIterator<>(graph, targetPackage));
            for (ArchipelagoPackage pkg : packages) {
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
