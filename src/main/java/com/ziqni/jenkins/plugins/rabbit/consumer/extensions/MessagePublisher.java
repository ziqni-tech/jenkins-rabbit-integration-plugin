package com.ziqni.jenkins.plugins.rabbit.consumer.extensions;

import com.rabbitmq.client.AMQP;
import hudson.Extension;
import hudson.ExtensionList;
import com.ziqni.jenkins.plugins.rabbit.consumer.RabbitManager;
import com.ziqni.jenkins.plugins.rabbit.consumer.channels.PublishRMQChannel;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishResult;
import jenkins.model.Jenkins;

import java.util.concurrent.Future;

/**
 * Extension class to publish message to RabbitMQ.
 *
 * @author rinrinne a.k.a. rin_ne
 */
@Extension
public class MessagePublisher {

    /**
     * Publish message.
     *
     * @param exchangeName the exhange name.
     * @param routingKey the routing key.
     * @param props the list of property.
     * @param body the content.
     * @return future object for PublishResult.
     */
    Future<PublishResult> publish(String exchangeName, String routingKey,
                                  AMQP.BasicProperties props, byte[] body) {
        PublishRMQChannel ch = RabbitManager.getInstance().getPublishChannel();
        if (ch != null && ch.isOpen()) {
            return ch.publish(exchangeName, routingKey, props, body);
        }
        return null;
    }

    /**
     * Get extension instance.
     *
     * @return the instance of this class.
     */
    public static MessagePublisher get() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            ExtensionList<MessagePublisher> extensions = jenkins.getExtensionList(MessagePublisher.class);
            if (extensions != null && extensions.size() > 0) {
                return extensions.get(0);
            }
        }
        return null;
    }
}
