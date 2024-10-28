package com.ziqni.jenkins.plugins.rabbit.console;

import com.rabbitmq.client.AMQP;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishChannel;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishChannelFactory;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishResult;
import com.ziqni.jenkins.plugins.rabbit.trigger.RabbitBuildTrigger;
import hudson.console.LineTransformationOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import static com.ziqni.jenkins.plugins.rabbit.trigger.RabbitBuildPublisher.LOG_HEADER;
import static com.ziqni.jenkins.plugins.rabbit.trigger.RabbitBuildPublisher.TEXT_CONTENT_TYPE;

public class RabbitLineLogger extends LineTransformationOutputStream.Delegating {

    private static final Logger LOGGER = Logger.getLogger(RabbitLineLogger.class.getName());
    private final RabbitConsoleCollectorJobProperty property;

    public RabbitLineLogger(OutputStream logger, RabbitConsoleCollectorJobProperty property) {
        super(logger); // Pass the underlying output stream to the superclass
        this.property = property;
    }

    @Override
    protected void eol(byte[] b, int len) throws IOException {
        // Convert the byte array to a string
        String line = new String(b, 0, len, StandardCharsets.UTF_8);

        // Trim any end-of-line characters (optional, based on your needs)
        line = trimEOL(line);

        // Process the line
        publish(line);
    }

    protected void publish(String line){

        // Basic property
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        builder.appId(RabbitBuildTrigger.PLUGIN_APPID);
        builder.contentType(TEXT_CONTENT_TYPE);

        // Publish message
        PublishChannel ch = PublishChannelFactory.getPublishChannel();
        if (ch != null && ch.isOpen()) {
            // return value is not needed if you don't need to wait.
            Future<PublishResult> future = ch.publish(
                    this.property.getExchangeName(),
                    this.property.getRoutingKey(),
                    builder.build(),
                    line.getBytes(StandardCharsets.UTF_8)
            );

            // Wait until publish is completed.
            try {
                PublishResult result = future.get();

                if (!result.isSuccess()) {
                    LOGGER.warning(LOG_HEADER + "Failed to publish message: " + line);
                }
            } catch (Exception e) {
                LOGGER.warning(e.getMessage());
            }
        }
    }
}
