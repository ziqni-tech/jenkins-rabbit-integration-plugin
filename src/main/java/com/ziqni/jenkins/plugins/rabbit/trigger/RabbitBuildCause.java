package com.ziqni.jenkins.plugins.rabbit.trigger;

import hudson.model.Cause;
import org.kohsuke.stapler.export.Exported;

/**
 * Cause class for remote build.
 *
 * @author rinrinne a.k.a. rin_ne
 */
public class RabbitBuildCause extends Cause {

    private final String queueName;

    /**
     * Creates instance with specified parameter.
     *
     * @param queueName
     *            the queue name.
     */
    public RabbitBuildCause(String queueName) {
        this.queueName = queueName;
    }

    @Override
    @Exported(visibility = 3)
    public String getShortDescription() {
        return "Triggered by remote build message from RabbitMQ queue: " + queueName;
    }

}
