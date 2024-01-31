package org.jenkinsci.plugins.scm_filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import jenkins.model.Jenkins;
import org.jenkinsci.plugin.gitea.GiteaSCMSource;
import org.junit.jupiter.api.Test;

class GiteaAgedRefsTraitTest {

    private GiteaSCMSource load(String file) throws IOException {
        try (InputStream res = getClass().getResourceAsStream(getClass().getSimpleName() + "/" + file)) {
            return (GiteaSCMSource) Jenkins.XSTREAM2.fromXML(res);
        }
    }

    @Test
    void restoreData() throws IOException {
        GiteaSCMSource instance = load("exclude_thirty_days.xml");
        assertThat(instance.getTraits())
          .singleElement()
          .isInstanceOf(GiteaAgedRefsTrait.class)
          .hasFieldOrPropertyWithValue("retentionDays", 30);
    }
}
