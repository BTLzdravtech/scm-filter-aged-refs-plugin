package org.jenkinsci.plugins.scm_filter;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import java.io.IOException;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.impl.trait.Selection;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugin.gitea.BranchSCMHead;
import org.jenkinsci.plugin.gitea.GiteaSCMSource;
import org.jenkinsci.plugin.gitea.GiteaSCMSourceContext;
import org.jenkinsci.plugin.gitea.GiteaSCMSourceRequest;
import org.jenkinsci.plugin.gitea.PullRequestSCMHead;
import org.jenkinsci.plugin.gitea.TagSCMHead;
import org.jenkinsci.plugin.gitea.client.api.GiteaBranch;
import org.jenkinsci.plugin.gitea.client.api.GiteaPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Sicco
 */
public class GiteaAgedRefsTrait extends AgedRefsTrait {

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
    public GiteaAgedRefsTrait(String branchRetentionDays, String prRetentionDays, String tagRetentionDays, String branchExcludeFilter) {
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
    @Symbol("giteaAgedRefsTrait")
    @SuppressWarnings("unused") // instantiated by Jenkins
    public static class DescriptorImpl extends AgedRefsDescriptorImpl {

        @Override
        public Class<? extends SCMSourceContext> getContextClass() {
            return GiteaSCMSourceContext.class;
        }

        @Override
        public Class<? extends SCMSource> getSourceClass() {
            return GiteaSCMSource.class;
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

                Iterable<GiteaBranch> branches = ((GiteaSCMSourceRequest) scmSourceRequest).getBranches();
                for (GiteaBranch branch : branches) {
                    if (branch.getName().equals(scmHead.getName())) {
                        long branchTS = branch.getCommit()
                          .getTimestamp()
                          .getTime();
                        return branchTS < super.getAcceptableBranchDateTimeThreshold();
                    }
                }
            } else if (scmHead instanceof PullRequestSCMHead && super.getAcceptableBranchDateTimeThreshold() > 0) {
                Iterable<GiteaPullRequest> pulls = ((GiteaSCMSourceRequest) scmSourceRequest).getPullRequests();
                for (GiteaPullRequest pull : pulls) {
                    if (("PR-" + pull.getNumber()).equals(scmHead.getName())) {
                        long pullTS = pull.getUpdatedAt()
                          .getTime();
                        return pullTS < super.getAcceptablePRDateTimeThreshold();
                    }
                }
            } else if (scmHead instanceof TagSCMHead && super.getAcceptableBranchDateTimeThreshold() > 0) {
                long tagTS = ((TagSCMHead) scmHead).getTimestamp();
                return tagTS < super.getAcceptableTagDateTimeThreshold();
            }
            return false;
        }
    }
}
