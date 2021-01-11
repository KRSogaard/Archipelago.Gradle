package build.archipelago.packageservice.core.delegates.createPackage;

import build.archipelago.account.common.AccountService;
import build.archipelago.account.common.exceptions.GitDetailsNotFound;
import build.archipelago.account.common.models.GitDetails;
import build.archipelago.common.exceptions.PackageExistsException;
import build.archipelago.common.github.GitService;
import build.archipelago.common.github.GitServiceFactory;
import build.archipelago.common.github.exceptions.GitRepoExistsException;
import build.archipelago.common.github.exceptions.RepoNotFoundException;
import build.archipelago.common.github.models.GitRepo;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.core.data.models.CreatePackageModel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreatePackageDelegate {

    private PackageData packageData;
    private AccountService accountService;
    private GitServiceFactory gitServiceFactory;

    public CreatePackageDelegate(PackageData packageData, AccountService accountService, GitServiceFactory gitServiceFactory) {
        this.packageData = packageData;
        this.accountService = accountService;
        this.gitServiceFactory = gitServiceFactory;
    }

    public void create(CreatePackageDelegateRequest request) throws PackageExistsException, GitDetailsNotFound {
        request.validate();

        GitDetails gitDetails = accountService.getGitDetails(request.getAccountId());

        GitService git = gitServiceFactory.getGitService(gitDetails.getCodeSource(),
                gitDetails.getGithubAccount(),
                gitDetails.getGitHubAccessToken());

        GitRepo repo;
        try {
            if (!git.hasRep(request.getName())) {
                log.info("The repo {} dose not exists", request.getName());
                repo = git.createRepo(request.getName(), request.getDescription(), true);
            } else {
                repo = git.getRepo(request.getName());
            }
        } catch (RepoNotFoundException ex) {
            log.error("Was told the repo {} exists but we where unable to fetch it", request.getName());
            throw new RuntimeException(ex);
        } catch (GitRepoExistsException ex) {
            log.error("Was told the repo {} did not exists, but we where not able to create it", request.getName());
            throw new RuntimeException(ex);
        }

        packageData.createPackage(request.getAccountId(), CreatePackageModel.builder()
                .name(request.getName())
                .description(request.getDescription())
                .gitCloneUrl(repo.getCloneUrl())
                .gitUrl(repo.getUrl())
                .build());
    }
}
