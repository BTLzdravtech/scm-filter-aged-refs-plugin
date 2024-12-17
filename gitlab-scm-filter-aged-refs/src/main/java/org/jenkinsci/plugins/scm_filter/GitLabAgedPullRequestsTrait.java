package org.jenkinsci.plugins.scm_filter;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSource;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceContext;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceRequest;
import io.jenkins.plugins.gitlabbranchsource.MergeRequestSCMHead;
import java.io.IOException;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.impl.trait.Selection;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.scm_filter.enums.RefType;
import org.jenkinsci.plugins.scm_filter.utils.GitLabFilterRefUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class GitLabAgedPullRequestsTrait extends AgedTypeRefsTrait {
    private static final RefType REF_TYPE = RefType.PULL_REQUEST;

    /**
     * Constructor for stapler.
     *
     * @param retentionDays retention period in days
     */
    @DataBoundConstructor
    public GitLabAgedPullRequestsTrait(String retentionDays) {
        super(retentionDays);
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (retentionDays > 0) {
            context.withFilter(new ExcludeOldPullRequestsSCMHeadFilter(retentionDays));
        }
    }

    @Extension
    @Selection
    @Symbol("gitLabAgedPullRequestsTrait")
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

        @Override
        @NonNull
        public String getDisplayName() {
            return "Filter pull requests by age";
        }

        @Override
        @NonNull
        public String getRefName() {
            return REF_TYPE.getName();
        }
    }

    /**
     * Filter that excludes pull requests according to their last commit modification date and the defined retentionDays.
     */
    private static class ExcludeOldPullRequestsSCMHeadFilter extends ExcludeReferencesSCMHeadFilter {

        ExcludeOldPullRequestsSCMHeadFilter(int retentionDays) {
            super(retentionDays);
        }

        @Override
        public boolean isExcluded(@NonNull SCMSourceRequest scmSourceRequest, @NonNull SCMHead scmHead)
                throws IOException, InterruptedException {
            if (scmHead instanceof MergeRequestSCMHead) {
                return GitLabFilterRefUtils.isPullRequestExcluded(
                        (GitLabSCMSourceRequest) scmSourceRequest,
                        (MergeRequestSCMHead) scmHead,
                        getAcceptableDateTimeThreshold());
            }
            return false;
        }
    }
}
