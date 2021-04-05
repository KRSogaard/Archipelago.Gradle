package build.archipelago.packageservice.core.delegates.getPackageCommits;

import build.archipelago.account.common.AccountService;
import build.archipelago.common.git.models.exceptions.GitDetailsNotFound;
import build.archipelago.account.common.models.GitDetails;
import build.archipelago.common.git.models.GitCommit;
import build.archipelago.common.github.GitService;
import build.archipelago.common.github.GitServiceFactory;
import build.archipelago.common.git.models.exceptions.BranchNotFoundException;
import build.archipelago.common.git.models.exceptions.RepoNotFoundException;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.models.GitCommitsResponse;
import build.archipelago.packageservice.models.PackageDetails;
import com.github.benmanes.caffeine.cache.Cache;

import java.util.List;

public class GetPackageCommitsDelegate {

    private PackageData packageData;
    private Cache<String, String> publicPackageAccountCache;
    private GitServiceFactory gitServiceFactory;
    private AccountService accountService;

    public GetPackageCommitsDelegate(PackageData packageData, Cache<String, String> publicPackageAccountCache,
                                     GitServiceFactory gitServiceFactory, AccountService accountService) {
        this.packageData = packageData;
        this.publicPackageAccountCache = publicPackageAccountCache;
        this.gitServiceFactory = gitServiceFactory;
        this.accountService = accountService;
    }

    public GitCommitsResponse get(String accountId, String pkg, String branch) throws PackageNotFoundException, GitDetailsNotFound, RepoNotFoundException, BranchNotFoundException {
        PackageDetails packageDetails;
        try {
            packageDetails = packageData.getPackageDetails(accountId, pkg);
        } catch (PackageNotFoundException exp) {
            try {
                String publicAccountId = publicPackageAccountCache.getIfPresent(pkg.toLowerCase());
                if (publicAccountId == null) {
                    publicAccountId = packageData.getPublicPackage(pkg.toLowerCase());
                    publicPackageAccountCache.put(pkg.toLowerCase(), publicAccountId);
                }
                packageDetails = packageData.getPackageDetails(publicAccountId, pkg);
            } catch (PackageNotFoundException e) {
                throw exp;
            }
        }

        GitDetails gitDetails = accountService.getGitDetails(accountId);
        GitService git = gitServiceFactory.getGitService(gitDetails.getCodeSource(),
                gitDetails.getGithubAccount(),
                gitDetails.getGitHubAccessToken());
        List<GitCommit> commits = git.getCommits(packageDetails.getGitRepoFullName(), branch);
        return GitCommitsResponse.builder().commits(commits).build();
    }

}
