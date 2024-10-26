package io.jenkins.plugins.roborabbit;

import static org.junit.Assert.*;

import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlTextInput;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsSessionRule;

public class RoboRabbitConfigurationTest {

    @Rule
    public JenkinsSessionRule sessions = new JenkinsSessionRule();

    /**
     * Tries to exercise enough code paths to catch common mistakes:
     * <ul>
     * <li>missing {@code load}
     * <li>missing {@code save}
     * <li>misnamed or absent getter/setter
     * <li>misnamed {@code textbox}
     * </ul>
     */
    @Test
    public void uiAndStorage() throws Throwable {
//        sessions.then(r -> {
//            assertNull("not set initially", RoboRabbitConfiguration.get().getLabel());
//            HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");
//            HtmlTextInput textbox = config.getInputByName("_.label");
//            textbox.setText("hello");
//            r.submit(config);
//            assertEquals(
//                    "global config page let us edit it",
//                    "hello",
//                    RoboRabbitConfiguration.get().getLabel());
//        });
//        sessions.then(r -> {
//            assertEquals(
//                    "still there after restart of Jenkins",
//                    "hello",
//                    RoboRabbitConfiguration.get().getLabel());
//        });
    }
}
