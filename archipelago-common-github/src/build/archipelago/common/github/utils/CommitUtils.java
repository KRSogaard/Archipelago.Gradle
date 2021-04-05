package build.archipelago.common.github.utils;

import build.archipelago.common.git.models.exceptions.BranchNotFoundException;
import build.archipelago.common.github.models.github.GithubCommit;

import java.util.*;

public class CommitUtils {

    public static List<GithubCommit> walkCommits(List<GithubCommit> list, String startingSha) {
        List<GithubCommit> orderedList = new ArrayList<>();
        Map<String, GithubCommit> map = new HashMap<>();
        for (GithubCommit commit : list) {
            map.put(commit.getSha(), commit);
        }
        Queue<String> fetchQueue = new LinkedList<>();
        fetchQueue.add(startingSha);
        while(!fetchQueue.isEmpty()) {
            String sha = fetchQueue.poll();
            if (!map.containsKey(sha)) {
                continue;
            }
            GithubCommit commit = map.get(sha);
            for(GithubCommit.Parent parent : commit.getParents()) {
                fetchQueue.add(parent.getSha());
            }
            orderedList.add(commit);
        }
        return orderedList;
    }
}
