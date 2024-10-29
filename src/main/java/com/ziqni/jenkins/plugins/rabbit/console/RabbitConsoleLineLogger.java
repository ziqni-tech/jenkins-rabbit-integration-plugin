package com.ziqni.jenkins.plugins.rabbit.console;

import com.rabbitmq.client.AMQP;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishChannel;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishChannelFactory;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishResult;
import com.ziqni.jenkins.plugins.rabbit.trigger.RabbitBuildTrigger;
import com.ziqni.jenkins.plugins.rabbit.utils.MachineIdentifier;
import com.ziqni.jenkins.plugins.rabbit.utils.RabbitMessageBuilder;
import com.ziqni.jenkins.plugins.rabbit.utils.Utils;
import hudson.console.LineTransformationOutputStream;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static com.ziqni.jenkins.plugins.rabbit.trigger.RabbitBuildPublisher.*;
import static com.ziqni.jenkins.plugins.rabbit.utils.MachineIdentifier.HEADER_MACHINE_ID;

public class RabbitConsoleLineLogger extends LineTransformationOutputStream.Delegating {

    private static final Logger LOGGER = Logger.getLogger(RabbitConsoleLineLogger.class.getName());

    private final RabbitConsoleBuildWrapper property;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final boolean hasStopPublishingIfMessageContains;
    private final Run<?,?> run;
    private final Map<String, String> envVars;
    private final boolean hasTemplate;

    private final AtomicBoolean isPublishing = new AtomicBoolean(true);

    public RabbitConsoleLineLogger(OutputStream logger, RabbitConsoleBuildWrapper property, Run<?,?> run, TaskListener listener) throws IOException, InterruptedException {
        super(logger); // Pass the underlying output stream to the superclass
        this.property = property;
        this.run = run;
        this.envVars = run.getEnvironment(listener);
        this.hasTemplate = property.getTemplate() != null && !property.getTemplate().trim().isEmpty() && !property.getTemplate().contains("$");

        // If the property is null or empty, set the publishing flag to true
        isPublishing.set(property.getStartPublishingIfMessageContains() == null || property.getStartPublishingIfMessageContains().trim().isEmpty());

        // Check if the property has a stop publishing message
        this.hasStopPublishingIfMessageContains = property.getStartPublishingIfMessageContains() != null && !property.getStartPublishingIfMessageContains().trim().isEmpty();
    }

    @Override
    protected void eol(byte[] b, int len) {
        try {
            // Increment the counter
            final var lineNumber = counter.incrementAndGet();

            // Convert the byte array to a string
            String line = new String(b, 0, len, StandardCharsets.UTF_8);

            // Trim any end-of-line characters (optional, based on your needs)
            line = trimEOL(line);

            if (!isPublishing.get()) {
                // Check if the line contains the specified string
                if (property.getStartPublishingIfMessageContains() != null && line.contains(property.getStartPublishingIfMessageContains())) {
                    isPublishing.set(true);
                }
            } else if (hasStopPublishingIfMessageContains && isPublishing.get() && Objects.nonNull(property.getStopPublishingIfMessageContains())) {
                if (line.contains(property.getStopPublishingIfMessageContains())) {
                    isPublishing.set(false);
                }
            }

            // Process the line if publishing is enabled
            if (isPublishing.get()) {
                String formattedLine = line;
                if (hasTemplate) {
                    String tmp = Utils.injectEnvVars(run, envVars, property.getTemplate());

                    if (tmp.contains("\"${BUILD_CONSOLE_LINE}\"")) {
                        tmp = tmp.replace("\"${BUILD_CONSOLE_LINE}\"", line.replace("\"", "\\\""));
                    } else {
                        tmp = tmp.replace("${BUILD_CONSOLE_LINE}", line);
                    }
                    formattedLine = tmp.replace("${BUILD_CONSOLE_LINE_NUMBER}", String.valueOf(lineNumber));
                }
                publish(formattedLine);
            }

            // Pass the original line to the underlying logger
            out.write(b, 0, len);

        } catch (Throwable e) {
            LOGGER.warning(LOG_HEADER + "Failed to process line: " + e.getMessage());
            // Make sure the exception doesn't interrupt logging
            try {
                out.write(b, 0, len);
            } catch (IOException ioException) {
                // Handle any secondary exceptions
                LOGGER.severe(LOG_HEADER + "Error writing to the original logger: " + ioException.getMessage());
            }
        }
    }


    protected void publish(String line){

        if(!isPublishing.get())
            return;

        // Headers
        Map<String,Object> headers = new HashMap<>();
        // Add a header with the line number
        headers.put("line-number", counter.get());
        // Add a header with the job name
        headers.put("job-name", run.getNumber());
        // Add a header with the display name of the run
        headers.put("display-name", run.getDisplayName());
        // Add a header to stop the message from being displayed in the console
        headers.put("stop-message-console", isPublishing.get() ? "false" : "true");
        // Add a header with the display name of the run
        headers.put(HEADER_MACHINE_ID, MachineIdentifier.getUniqueMachineId());

        // Basic property
        AMQP.BasicProperties.Builder builder = RabbitMessageBuilder.build(new AMQP.BasicProperties.Builder(), this.property, headers);

        builder.appId(RabbitBuildTrigger.PLUGIN_APPID);
        builder.contentType(TEXT_CONTENT_TYPE);

        // Publish message
        PublishChannel ch = PublishChannelFactory.getPublishChannel();
        if (ch != null && ch.isOpen()) {
            // return value is not needed if you don't need to wait.
            Future<PublishResult> future = ch.publish(
                    this.property.getExchangeName(),
                    Utils.injectEnvVars(run, envVars, this.property.getRoutingKey()),
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
