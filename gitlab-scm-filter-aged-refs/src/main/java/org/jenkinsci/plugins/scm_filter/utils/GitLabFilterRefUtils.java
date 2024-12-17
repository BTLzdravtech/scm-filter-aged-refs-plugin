package org.jenkinsci.plugins.scm_filter.utils;

import io.jenkins.plugins.gitlabbranchsource.BranchSCMHead;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceRequest;
import io.jenkins.plugins.gitlabbranchsource.GitLabTagSCMHead;
import io.jenkins.plugins.gitlabbranchsource.MergeRequestSCMHead;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.MergeRequest;

public class GitLabFilterRefUtils {
    public static boolean isBranchExcluded(
            GitLabSCMSourceRequest scmSourceRequest, BranchSCMHead scmHead, long acceptableDateTimeThreshold)
            throws IOException {
        Iterable<Branch> branches = scmSourceRequest.getBranches();
        for (Branch branch : branches) {
            long branchTS = branch.getCommit()
                    .getCommittedDate()
                    .getTime();
            if (branch.getName().equals(scmHead.getName())) {
                return branchTS < acceptableDateTimeThreshold;
            }
        }
        return false;
    }

    public static boolean isPullRequestExcluded(
            GitLabSCMSourceRequest scmSourceRequest, MergeRequestSCMHead scmHead, long acceptableDateTimeThreshold) {
            String pullNr = scmHead.getId();
        Iterable<MergeRequest> mrs = scmSourceRequest.getMergeRequests();
        Optional<MergeRequest> mr = StreamSupport.stream(mrs.spliterator(), false)
                .filter(p -> pullNr.equals(Long.toString(p.getId())))
                .findAny();
        return mr.filter(mergeRequest -> isMrExcluded(scmSourceRequest, mergeRequest, acceptableDateTimeThreshold)).isPresent();
    }

    private static boolean isMrExcluded(GitLabSCMSourceRequest gitLabSCMSourceRequest, MergeRequest mr, long acceptableDateTimeThreshold) {
        GitLabApi api = gitLabSCMSourceRequest.getGitLabApi();
        if (api == null) {
            return false;
        }
        try {
            Commit commit = api.getCommitsApi().getCommit(mr.getSourceProjectId(), mr.getSha());
            long pullTS = commit.getCommittedDate().getTime();
            return pullTS < acceptableDateTimeThreshold;
        } catch (GitLabApiException e) {
            return false;
        }
    }

    public static boolean isTagExcluded(GitLabTagSCMHead scmHead, long acceptableDateTimeThreshold) {
        long tagTS = scmHead.getTimestamp();
        return tagTS < acceptableDateTimeThreshold;
    }
}
