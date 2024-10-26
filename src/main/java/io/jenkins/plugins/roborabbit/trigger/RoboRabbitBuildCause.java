package io.jenkins.plugins.roborabbit.trigger;

import hudson.model.Cause;
import org.kohsuke.stapler.export.Exported;

/**
 * Cause class for remote build.
 *
 * @author rinrinne a.k.a. rin_ne
 */
public class RoboRabbitBuildCause extends Cause {

    private final String queueName;

    /**
     * Creates instance with specified parameter.
     *
     * @param queueName
     *            the queue name.
     */
    public RoboRabbitBuildCause(String queueName) {
        this.queueName = queueName;
    }

    @Override
    @Exported(visibility = 3)
    public String getShortDescription() {
        return "Triggered by remote build message from RabbitMQ queue: " + queueName;
    }

}
