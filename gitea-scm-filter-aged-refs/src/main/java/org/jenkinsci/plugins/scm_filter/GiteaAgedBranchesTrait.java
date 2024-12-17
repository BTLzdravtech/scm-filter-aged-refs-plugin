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
import org.jenkinsci.plugins.scm_filter.enums.RefType;
import org.jenkinsci.plugins.scm_filter.utils.GiteaFilterRefUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class GiteaAgedBranchesTrait extends AgedTypeRefsTrait {
    private static final RefType REF_TYPE = RefType.BRANCH;

    /**
     * Constructor for stapler.
     *
     * @param retentionDays retention period in days
     */
    @DataBoundConstructor
    public GiteaAgedBranchesTrait(String retentionDays) {
        super(retentionDays);
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (retentionDays > 0) {
            context.withFilter(new ExcludeOldBranchesSCMHeadFilter(retentionDays));
        }
    }

    @Extension
    @Selection
    @Symbol("giteaAgedBranchesTrait")
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

        @Override
        @NonNull
        public String getDisplayName() {
            return "Filter branches by age";
        }

        @Override
        @NonNull
        public String getRefName() {
            return REF_TYPE.getName();
        }
    }

    /**
     * Filter that excludes branches according to their last commit modification date and the defined retentionDays.
     */
    private static class ExcludeOldBranchesSCMHeadFilter extends ExcludeReferencesSCMHeadFilter {

        ExcludeOldBranchesSCMHeadFilter(int retentionDays) {
            super(retentionDays);
        }

        @Override
        public boolean isExcluded(@NonNull SCMSourceRequest scmSourceRequest, @NonNull SCMHead scmHead)
                throws IOException, InterruptedException {
            if (scmHead instanceof BranchSCMHead) {
                return GiteaFilterRefUtils.isBranchExcluded(
                        (GiteaSCMSourceRequest) scmSourceRequest,
                        (BranchSCMHead) scmHead,
                        getAcceptableDateTimeThreshold());
            }
            return false;
        }
    }
}
