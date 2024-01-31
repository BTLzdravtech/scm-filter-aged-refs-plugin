package org.jenkinsci.plugins.scm_filter;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import io.jenkins.plugins.gitlabbranchsource.BranchSCMHead;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSource;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceContext;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceRequest;
import io.jenkins.plugins.gitlabbranchsource.GitLabTagSCMHead;
import io.jenkins.plugins.gitlabbranchsource.MergeRequestSCMHead;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.impl.trait.Selection;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.MergeRequest;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class GitLabAgedRefsTrait extends AgedRefsTrait {
    public static final Logger LOGGER = Logger.getLogger(GitLabAgedRefsTrait.class.getName());

    /**
     * Constructor for stapler.
     *
     * @param branchRetentionDays retention period in days for branches
     * @param prRetentionDays     retention period in days for pull requests
     * @param tagRetentionDays    retention period in days for tags
     * @param branchExcludeFilter space-separated list of branch name patterns to
     *                            ignore. For example: release main hotfix-*
     */
    @DataBoundConstructor
    public GitLabAgedRefsTrait(String branchRetentionDays, String prRetentionDays, String tagRetentionDays, String branchExcludeFilter) {
        super(branchRetentionDays, prRetentionDays, tagRetentionDays, branchExcludeFilter);
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        context.withFilter(new ExcludeOldBranchesSCMHeadFilter(branchRetentionDays, prRetentionDays, tagRetentionDays, branchExcludeFilter));
    }

    /**
     * Our descriptor.
     */
    @Extension
    @Selection
    @Symbol("gitLabAgedRefsTrait")
    @SuppressWarnings("unused") // instantiated by Jenkins
    public static class DescriptorImpl extends AgedRefsDescriptorImpl {

        @Override
        public Class<? extends SCMSourceContext> getContextClass() {
            return GitLabSCMSourceContext.class;
        }

        @Override
        public Class<? extends SCMSource> getSourceClass() {
            return GitLabSCMSource.class;
        }
    }

    /**
     * Filter that excludes references (branches, pull requests, tags) according to
     * their last commit modification date and the defined branchRetentionDays.
     */
    private static class ExcludeOldBranchesSCMHeadFilter extends ExcludeBranchesSCMHeadFilter {

        ExcludeOldBranchesSCMHeadFilter(int branchRetentionDays, int prRetentionDays, int tagRetentionDays, String branchExcludeFilter) {
            super(branchRetentionDays, prRetentionDays, tagRetentionDays, branchExcludeFilter);
        }

        @Override
        public boolean isExcluded(@NonNull SCMSourceRequest scmSourceRequest, @NonNull SCMHead scmHead)
                throws IOException {
            if (scmHead instanceof BranchSCMHead && super.getAcceptableBranchDateTimeThreshold() > 0) {
                if (scmHead.getName().matches(super.getBranchExcludePattern())) {
                    return false;
                }

                Iterable<Branch> branches = ((GitLabSCMSourceRequest) scmSourceRequest).getBranches();
                for (Branch branch : branches) {
                    if (branch.getName().equals(scmHead.getName())) {
                        long branchTS = branch.getCommit().getCommittedDate().getTime();
                        return branchTS < getAcceptableBranchDateTimeThreshold();
                    }
                }
            } else if (scmHead instanceof MergeRequestSCMHead && super.getAcceptableBranchDateTimeThreshold() > 0) {
                MergeRequestSCMHead mrHead = (MergeRequestSCMHead) scmHead;
                GitLabSCMSourceRequest gitLabSCMSourceRequest = (GitLabSCMSourceRequest) scmSourceRequest;
                Iterable<MergeRequest> mrs = gitLabSCMSourceRequest.getMergeRequests();
                for (MergeRequest mr : mrs) {
                    if (Long.toString(mr.getId()).equals(mrHead.getId())) {
                        return isMrExcluded(gitLabSCMSourceRequest, mr);
                    }
                }
            } else if (scmHead instanceof GitLabTagSCMHead && super.getAcceptableBranchDateTimeThreshold() > 0) {
                long tagTS = ((GitLabTagSCMHead) scmHead).getTimestamp();
                return tagTS < super.getAcceptableTagDateTimeThreshold();
            }
            return false;
        }

        private boolean isMrExcluded(GitLabSCMSourceRequest gitLabSCMSourceRequest, MergeRequest mr) {
            GitLabApi api = gitLabSCMSourceRequest.getGitLabApi();
            if (api == null) {
                LOGGER.log(Level.FINEST, "No GitLab API?!?");
                return false;
            }
            try {
                Commit commit = api.getCommitsApi().getCommit(mr.getSourceProjectId(), mr.getSha());
                long pullTS = commit.getCommittedDate().getTime();
                return pullTS < getAcceptablePRDateTimeThreshold();
            } catch (GitLabApiException e) {
                LOGGER.log(Level.FINE, e, () -> "Cannot resolve commit " + mr.getSha() + " for MR " + mr.getId());
                return false;
            }
        }
    }
}
