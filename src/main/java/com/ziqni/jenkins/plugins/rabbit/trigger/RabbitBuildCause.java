package com.ziqni.jenkins.plugins.rabbit.trigger;

import com.ziqni.jenkins.plugins.rabbit.utils.RabbitMessageProperties;
import hudson.model.Cause;
import org.kohsuke.stapler.export.Exported;

/**
 * Cause class for remote build.
 *
 * @author rinrinne a.k.a. rin_ne
 */
public class RabbitBuildCause extends Cause {

    private final RabbitMessageProperties rabbitMessageProperties;

    /**
     * Creates instance with specified parameter.
     *
     * @param rabbitMessageProperties the message properties.
     */
    public RabbitBuildCause(RabbitMessageProperties rabbitMessageProperties) {
        this.rabbitMessageProperties = rabbitMessageProperties;
    }

    @Override
    @Exported(visibility = 3)
    public String getShortDescription() {
        return "Triggered by remote build message from RabbitMQ queue: " + rabbitMessageProperties.getQueueName();
    }

    /**
     * Gets the RabbitMessageProperties associated with this cause.
     * @return the RabbitMessageProperties
     */
    public RabbitMessageProperties getRabbitMessageProperties() {
        return rabbitMessageProperties;
    }
}
