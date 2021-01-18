package build.archipelago.packageservice.configuration;

import build.archipelago.account.common.AccountService;
import build.archipelago.common.github.GitServiceFactory;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.core.delegates.createPackage.CreatePackageDelegate;
import build.archipelago.packageservice.core.delegates.getBuildArtifact.GetBuildArtifactDelegate;
import build.archipelago.packageservice.core.delegates.getPackage.GetPackageDelegate;
import build.archipelago.packageservice.core.delegates.getPackageBuild.GetPackageBuildDelegate;
import build.archipelago.packageservice.core.delegates.getPackageBuildByGit.GetPackageBuildByGitDelegate;
import build.archipelago.packageservice.core.delegates.getPackageBuilds.GetPackageBuildsDelegate;
import build.archipelago.packageservice.core.delegates.getPackages.GetPackagesDelegate;
import build.archipelago.packageservice.core.delegates.uploadBuildArtifact.UploadBuildArtifactDelegate;
import build.archipelago.packageservice.core.delegates.verifyBuildsExists.VerifyBuildsExistsDelegate;
import build.archipelago.packageservice.core.delegates.verifyPackageExists.VerifyPackageExistsDelegate;
import build.archipelago.packageservice.core.storage.PackageStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;

@Configuration
@Slf4j
public class DelegateConfiguration {

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public UploadBuildArtifactDelegate uploadPackageDelegate(
            PackageData packageData,
            PackageStorage packageStorage) {
        return new UploadBuildArtifactDelegate(packageData, packageStorage);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public GetBuildArtifactDelegate getBuildArtifactDelegate(
            PackageData packageData,
            PackageStorage packageStorage) {
        return new GetBuildArtifactDelegate(packageData, packageStorage);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public CreatePackageDelegate createPackageDelegate(
            PackageData packageData,
            AccountService accountService,
            GitServiceFactory gitServiceFactory) {
        return new CreatePackageDelegate(packageData, accountService, gitServiceFactory);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public GetPackageDelegate getPackageDelegate(
            PackageData packageData) {
        return new GetPackageDelegate(packageData);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public GetPackageBuildsDelegate getPackageBuildsDelegate(
            PackageData packageData) {
        return new GetPackageBuildsDelegate(packageData);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public GetPackageBuildDelegate getPackageBuildDelegate(
            PackageData packageData) {
        return new GetPackageBuildDelegate(packageData);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public VerifyBuildsExistsDelegate verifyBuildsExistsDelegate(
            PackageData packageData) {
        return new VerifyBuildsExistsDelegate(packageData);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public VerifyPackageExistsDelegate verifyPackageExistsDelegate(
            PackageData packageData) {
        return new VerifyPackageExistsDelegate(packageData);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public GetPackageBuildByGitDelegate getPackageBuildByGitDelegate(
            PackageData packageData) {
        return new GetPackageBuildByGitDelegate(packageData);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public GetPackagesDelegate getPackagesDelegate(
            PackageData packageData) {
        return new GetPackagesDelegate(packageData);
    }
}
