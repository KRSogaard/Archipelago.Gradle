package build.archipelago.maui.core.actions;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.git.models.exceptions.RepoNotFoundException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.common.PackageSourceProvider;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.models.PackageDetails;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class WorkspaceUseAction extends BaseAction {

    private HarborClient harborClient;
    private PackageSourceProvider packageSourceProvider;

    public WorkspaceUseAction(WorkspaceContextFactory workspaceContextFactory,
                              SystemPathProvider systemPathProvider,
                              OutputWrapper out,
                              HarborClient harborClient,
                              PackageSourceProvider packageSourceProvider) {
        super(workspaceContextFactory, systemPathProvider, out);
        this.harborClient = harborClient;
        this.packageSourceProvider = packageSourceProvider;
    }

    public boolean useVersionSet(String versionSet) {
        Preconditions.checkNotNull(versionSet);

        if (!setupWorkspaceContext()) {
            out.error("Was unable to locate the workspace");
            return false;
        }

        if (versionSet.equalsIgnoreCase(workspaceContext.getVersionSet())) {
            out.error("The version-set for the workspace is already \"%s\"", workspaceContext.getVersionSet());
            return false;
        } else {
            try {
                VersionSet vs = harborClient.getVersionSet(versionSet);
                out.write("Setting the version-set for the workspace to \"%s\"", vs.getName());
                // Ensure we have the right capitalization of the version-set
                workspaceContext.setVersionSet(vs.getName());
                workspaceContext.save();
                // We clear the cache as it would have been the old version-set
                workspaceContext.clearVersionSetRevisionCache();
            } catch (VersionSetDoseNotExistsException e) {
                out.error("The version-set \"%s\" dose not exists", versionSet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    public boolean usePackages(List<String> packages) {
        Preconditions.checkNotNull(packages);

        if (!setupWorkspaceContext()) {
            out.error("Was unable to locate the workspace");
            return false;
        }

        for (final String pkg : packages) {
            if (!ArchipelagoPackage.validateName(pkg)) {
                out.error("The package name \"%s\" is not valid", pkg);
                continue;
            }
            try {
                // We used getLocalPackages here as it will nto throw a PackageNotLocalException in case the ISLAND file is missing
                if (workspaceContext.getLocalPackages().stream().anyMatch(lp -> lp.equalsIgnoreCase(pkg))) {
                    out.error("The package name \"%s\" already checked out", pkg);
                    continue;
                }
                PackageDetails aPackage = harborClient.getPackage(pkg);
                if (Files.exists(wsDir.resolve(aPackage.getName()))) {
                    out.error("Directory %s already exists, please remove or rename it " +
                            "before checking out the package %s", aPackage.getName(), aPackage.getName());
                    continue;
                }

                if (!packageSourceProvider.checkOutSource(wsDir, aPackage)) {
                    out.error("Failed to checkout the source for the package \"%s\"", pkg);
                    continue;
                }

                workspaceContext.addLocalPackage(aPackage.getName());
                workspaceContext.save();

                out.write("Successfully added %s to the workspace", aPackage.getName());
            } catch (PackageNotFoundException exp) {
                out.error("The package name \"%s\" dose not exists", pkg);
            } catch (RepoNotFoundException e) {
                out.error("The repo for package \"%s\" was not found", pkg);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }
}
