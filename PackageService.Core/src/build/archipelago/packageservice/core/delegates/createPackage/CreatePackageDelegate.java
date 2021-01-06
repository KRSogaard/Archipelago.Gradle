package build.archipelago.packageservice.core.delegates.createPackage;

import build.archipelago.account.common.AccountService;
import build.archipelago.account.common.exceptions.GitDetailsNotFound;
import build.archipelago.account.common.models.GitDetails;
import build.archipelago.common.exceptions.PackageExistsException;
import build.archipelago.common.github.GitService;
import build.archipelago.common.github.GitServiceFactory;
import build.archipelago.common.github.exceptions.GitRepoExistsException;
import build.archipelago.common.github.exceptions.NotFoundException;
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

        GitRepo repo = null;
        if (!git.hasRep(request.getName())) {
            log.info("The repo {} dose not exists", request.getName());
            try {
                git.createRepo(request.getName(), request.getDescription(), true);
            } catch (GitRepoExistsException e) {
                log.error("Not repo exists for {}, but we already checked if it exists and got no. " +
                        "Trying to fetch again", request.getName());
                try {
                    repo = git.getRepo(request.getName());
                } catch (NotFoundException ex) {
                    log.error("Was told the repo {} exists but we where unable to fetch it", request.getName());
                    throw new RuntimeException(ex);
                }
            }
        } else {
            try {
                repo = git.getRepo(request.getName());
            } catch (NotFoundException ex) {
                log.error("Was told the repo {} exists but we where unable to fetch it", request.getName());
                throw new RuntimeException(ex);
            }
        }

        packageData.createPackage(request.getAccountId(), CreatePackageModel.builder()
                .name(request.getName())
                .description(request.getDescription())
                .gitCloneUrl(repo.getCloneUrl())
                .gitUrl(repo.getUrl())
                .build());
    }
}
