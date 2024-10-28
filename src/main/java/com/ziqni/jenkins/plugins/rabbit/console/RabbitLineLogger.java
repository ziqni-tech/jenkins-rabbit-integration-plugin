package com.ziqni.jenkins.plugins.rabbit.console;

import com.rabbitmq.client.AMQP;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishChannel;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishChannelFactory;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishResult;
import com.ziqni.jenkins.plugins.rabbit.trigger.RabbitBuildTrigger;
import hudson.console.LineTransformationOutputStream;
import hudson.model.Run;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static com.ziqni.jenkins.plugins.rabbit.trigger.RabbitBuildPublisher.LOG_HEADER;
import static com.ziqni.jenkins.plugins.rabbit.trigger.RabbitBuildPublisher.TEXT_CONTENT_TYPE;

public class RabbitLineLogger extends LineTransformationOutputStream.Delegating {

    private static final Logger LOGGER = Logger.getLogger(RabbitLineLogger.class.getName());
    private final RabbitConsoleCollectorJobProperty property;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final boolean hasStopPublishingIfMessageContains;
    private final Run build;

    private final AtomicBoolean isPublishing = new AtomicBoolean(false);

    public RabbitLineLogger(OutputStream logger, RabbitConsoleCollectorJobProperty property, Run build) {
        super(logger); // Pass the underlying output stream to the superclass
        this.property = property;
        this.build = build;

        // If the property is null or empty, set the publishing flag to true
        if(property.getStartPublishingIfMessageContains() == null && property.getStartPublishingIfMessageContains().trim().isEmpty()){
            isPublishing.set(true);
        }

        // Check if the property has a stop publishing message
        this.hasStopPublishingIfMessageContains = property.getStartPublishingIfMessageContains() != null && !property.getStartPublishingIfMessageContains().trim().isEmpty();
    }

    @Override
    protected void eol(byte[] b, int len) throws IOException {

        // Increment the counter
        counter.incrementAndGet();

        // Convert the byte array to a string
        String line = new String(b, 0, len, StandardCharsets.UTF_8);

        // Trim any end-of-line characters (optional, based on your needs)
        line = trimEOL(line);

        if(!isPublishing.get()){
            // Check if the line contains the specified string
            if (line.contains(property.getStartPublishingIfMessageContains())) {
                isPublishing.set(true);
            }
        }
        else if(hasStopPublishingIfMessageContains && isPublishing.get()) {
            if (line.contains(property.getStopPublishingIfMessageContains())) {
                isPublishing.set(false);
            }
        }

        // Process the line
        if(isPublishing.get()) {
            publish(line);
        }
    }

    protected void publish(String line){

        if(!isPublishing.get())
            return;

        // Basic property
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        builder.appId(RabbitBuildTrigger.PLUGIN_APPID);
        builder.contentType(TEXT_CONTENT_TYPE);

        // Headers
        Map<String,Object> headers = new HashMap<>();
        headers.put("line-number", counter.get());
        headers.put("job-name", build.getNumber());
        headers.put("display-name", build.getDisplayName());
        builder.headers(headers);

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
