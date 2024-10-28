package com.ziqni.jenkins.plugins.rabbit.consumer.publishers;

import com.ziqni.jenkins.plugins.rabbit.consumer.RabbitManager;

/**
 * A factory class for RabbitMQ publish channel.
 *
 * @author rinrinne a.k.a. rin_ne
 */
public class PublishChannelFactory {

    private PublishChannelFactory() {
    }

    /**
     * Gets {@link PublishChannel}.
     * Note that you should not keep this instance.
     *
     * @return a instance.
     */
    public static PublishChannel getPublishChannel() {
        return RabbitManager.getInstance().getPublishChannel();
    }
}
