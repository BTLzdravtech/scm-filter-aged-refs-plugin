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
     * @param retentionDays retention period in days
     */
    @DataBoundConstructor
    public GiteaAgedRefsTrait(String retentionDays) {
        super(retentionDays);
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (retentionDays > 0) {
            context.withFilter(new ExcludeOldBranchesSCMHeadFilter(retentionDays));
        }
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
     * Filter that excludes references (branches, pull requests, tags) according to their last commit modification date and the defined retentionDays.
     */
    private static class ExcludeOldBranchesSCMHeadFilter extends ExcludeBranchesSCMHeadFilter {

        ExcludeOldBranchesSCMHeadFilter(int retentionDays) {
            super(retentionDays);
        }

        @Override
        public boolean isExcluded(@NonNull SCMSourceRequest scmSourceRequest, @NonNull SCMHead scmHead)
          throws IOException {
            if (scmHead instanceof BranchSCMHead) {
                Iterable<GiteaBranch> branches = ((GiteaSCMSourceRequest) scmSourceRequest).getBranches();
                for (GiteaBranch branch : branches) {
                    long branchTS = branch.getCommit()
                      .getTimestamp()
                      .getTime();
                    if (branch.getName().equals(scmHead.getName())) {
                        return branchTS < super.getAcceptableDateTimeThreshold();
                    }
                }
            } else if (scmHead instanceof PullRequestSCMHead) {
                Iterable<GiteaPullRequest> pulls = ((GiteaSCMSourceRequest) scmSourceRequest).getPullRequests();
                for (GiteaPullRequest pull : pulls) {
                    if (("PR-" + pull.getNumber()).equals(scmHead.getName())) {
                        long pullTS = pull.getUpdatedAt()
                          .getTime();
                        return pullTS < super.getAcceptableDateTimeThreshold();
                    }
                }
            } else if (scmHead instanceof TagSCMHead) {
                long tagTS = ((TagSCMHead) scmHead).getTimestamp();
                return tagTS < super.getAcceptableDateTimeThreshold();
            }
            return false;
        }
    }
}
