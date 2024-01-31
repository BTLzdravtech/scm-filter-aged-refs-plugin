package org.jenkinsci.plugins.scm_filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import jenkins.model.Jenkins;
import jenkins.scm.api.SCMSource;
import org.hamcrest.Matchers;
import org.jenkinsci.plugin.gitea.GiteaSCMSource;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.jvnet.hudson.test.JenkinsRule;

public class GiteaAgedRefsTraitTest {

    @ClassRule
    public static final JenkinsRule j = new JenkinsRule();

    @Rule
    public TestName currentTestName = new TestName();

    private SCMSource load() {
        return load(currentTestName.getMethodName());
    }

    private SCMSource load(String dataSet) {
        return (GiteaSCMSource)
          Jenkins.XSTREAM2.fromXML(getClass().getResource(getClass().getSimpleName() + "/" + dataSet + ".xml"));
    }

    @Test
    public void plugin_defaults() {
        GiteaSCMSource instance = (GiteaSCMSource) load();
        assertThat(
          instance.getTraits(),
          contains(Matchers.allOf(
            instanceOf(GiteaAgedRefsTrait.class),
            hasProperty("branchRetentionDays", is(0)),
            hasProperty("prRetentionDays", is(0)),
            hasProperty("tagRetentionDays", is(0)),
            hasProperty("branchExcludeFilter", is("")))));
    }

    @Test
    public void plugin_enabled() {
        GiteaSCMSource instance = (GiteaSCMSource) load();
        assertThat(
          instance.getTraits(),
          contains(Matchers.allOf(
            instanceOf(GiteaAgedRefsTrait.class),
            hasProperty("branchRetentionDays", is(30)),
            hasProperty("prRetentionDays", is(40)),
            hasProperty("tagRetentionDays", is(50)),
            hasProperty("branchExcludeFilter", is("main hotfix-*")))));
    }
}
