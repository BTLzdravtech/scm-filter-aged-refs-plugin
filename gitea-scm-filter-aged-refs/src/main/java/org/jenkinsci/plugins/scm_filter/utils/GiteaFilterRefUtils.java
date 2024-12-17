package org.jenkinsci.plugins.scm_filter.utils;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.jenkinsci.plugin.gitea.BranchSCMHead;
import org.jenkinsci.plugin.gitea.GiteaSCMSourceRequest;
import org.jenkinsci.plugin.gitea.PullRequestSCMHead;
import org.jenkinsci.plugin.gitea.TagSCMHead;
import org.jenkinsci.plugin.gitea.client.api.GiteaBranch;
import org.jenkinsci.plugin.gitea.client.api.GiteaPullRequest;

public class GiteaFilterRefUtils {
    public static boolean isBranchExcluded(
      GiteaSCMSourceRequest scmSourceRequest, BranchSCMHead scmHead, long acceptableDateTimeThreshold)
            throws IOException {
        Iterable<GiteaBranch> branches = scmSourceRequest.getBranches();
        for (GiteaBranch branch : branches) {
            long branchTS = branch.getCommit()
                    .getTimestamp()
                    .getTime();
            if (branch.getName().equals(scmHead.getName())) {
                return branchTS < acceptableDateTimeThreshold;
            }
        }
        return false;
    }

    public static boolean isPullRequestExcluded(
            GiteaSCMSourceRequest scmSourceRequest, PullRequestSCMHead scmHead, long acceptableDateTimeThreshold) {
        String pullNr = scmHead.getName();
        Iterable<GiteaPullRequest> pulls = scmSourceRequest.getPullRequests();
        Optional<GiteaPullRequest> pull = StreamSupport.stream(pulls.spliterator(), false)
                .filter(p -> pullNr.equals("PR-" + p.getNumber()))
                .findAny();
        if (pull.isPresent()) {
            long latestPullTS = pull.get().getUpdatedAt().getTime();
            if (latestPullTS > 0) return latestPullTS < acceptableDateTimeThreshold;
        }
        return false;
    }

    public static boolean isTagExcluded(TagSCMHead scmHead, long acceptableDateTimeThreshold) {
        long tagTS = scmHead.getTimestamp();
        return tagTS < acceptableDateTimeThreshold;
    }
}
