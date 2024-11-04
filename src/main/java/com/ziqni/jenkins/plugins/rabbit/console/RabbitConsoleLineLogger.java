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
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static com.ziqni.jenkins.plugins.rabbit.trigger.RabbitBuildPublisher.*;
import static com.ziqni.jenkins.plugins.rabbit.utils.MachineIdentifier.HEADER_MACHINE_ID;

public class RabbitConsoleLineLogger extends LineTransformationOutputStream.Delegating {

    private static final Logger LOGGER = Logger.getLogger(RabbitConsoleLineLogger.class.getName());

    private final Run<?,?> run;
    private final Map<String, String> envVars;
    private final RabbitConsoleBuildWrapper property;

    private final boolean hasTemplate;
    private final boolean hasStartPublishingIfMessageContains;
    private final boolean hasStopPublishingIfMessageContains;

    private final List<String> logLines = new LinkedList<>();
    private final AtomicInteger counter = new AtomicInteger(0);
    private final AtomicBoolean remoteLoggingEnabled = new AtomicBoolean(true);

    public RabbitConsoleLineLogger(OutputStream logger, RabbitConsoleBuildWrapper property, Run<?,?> run, TaskListener listener) throws IOException, InterruptedException {
        super(logger); // Pass the underlying output stream to the superclass
        this.property = property;
        this.run = run;
        this.envVars = run.getEnvironment(listener);
        this.hasTemplate = property.getTemplate() != null && !property.getTemplate().trim().isEmpty() && !property.getTemplate().contains("$");

        // Check if the property has a stop publishing message
        this.hasStartPublishingIfMessageContains = property.getStartPublishingIfMessageContains() != null && !property.getStartPublishingIfMessageContains().trim().isEmpty();

        // Check if the property has a stop publishing message
        this.hasStopPublishingIfMessageContains = property.getStopPublishingIfMessageContains() != null && !property.getStopPublishingIfMessageContains().trim().isEmpty();

        // If the property is null or empty, set the publishing flag to true
        remoteLoggingEnabled.set(!hasStartPublishingIfMessageContains);
    }

    @Override
    protected void eol(byte[] b, int len) {
        try {

            // Pass the original line to the underlying logger
            out.write(b, 0, len);

            // Increment the counter
            final var lineNumber = counter.incrementAndGet();

            // Convert the byte array to a string
            String line = new String(b, 0, len, StandardCharsets.UTF_8);

            // Trim any end-of-line characters (optional, based on your needs)
            line = trimEOL(line);

            // Create a new line handler
            LineHandler lineHandler = new LineHandler(logLines);

            // Do publishing to rabbit
            boolean publishNow = false;

            if (!remoteLoggingEnabled.get()) {
                // Check if the line contains the specified string
                if (property.getStartPublishingIfMessageContains() != null && line.contains(property.getStartPublishingIfMessageContains())) {
                    remoteLoggingEnabled.set(true);

                    if(Objects.isNull(property.getExcludeStartLine()) || property.getExcludeStartLine()) {
                        lineHandler.add(line);
                    }
                }
            }
            else if (hasStopPublishingIfMessageContains && remoteLoggingEnabled.get() && Objects.nonNull(property.getStopPublishingIfMessageContains())) {

                if (line.contains(property.getStopPublishingIfMessageContains())) {
                    remoteLoggingEnabled.set(false);
                    publishNow = true;

                    if (Objects.isNull(property.getExcludeStopLine()) || !property.getExcludeStopLine()) {
                        lineHandler.add(line);
                    }
                }
            }

            // Process the line if publishing is enabled
            if (remoteLoggingEnabled.get()) {
                logLines.add(line);
            }

            if(lineHandler.hasLines() && !property.getEnableBundling()){
                publish(run, format(lineHandler));
            }
            else if(lineHandler.hasLines() && publishNow){
                publish(run, format(lineHandler));
            }

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

    public String format(LineHandler lineHandler) {

        String lines = lineHandler.getLines();

        if (hasTemplate) {
            String tmp = Utils.injectEnvVars(run, envVars, property.getTemplate());

            if (tmp.contains("\"${BUILD_CONSOLE_LINE}\"")) {
                tmp = tmp.replace("\"${BUILD_CONSOLE_LINE}\"", lines.replace("\"", "\\\""));
            } else {
                tmp = tmp.replace("${BUILD_CONSOLE_LINE}", lines);
            }
            lines = tmp.replace("${BUILD_CONSOLE_LINE_NUMBER}", String.valueOf(counter.get()));
        }

        return lines;
    }

    protected void publish(Run<?, ?> run, String lines){

        if(!remoteLoggingEnabled.get())
            return;

        // Headers
        Map<String,Object> headers = new HashMap<>();
        // Add a header with the line number
        headers.put("line-number", counter.get());
        // Add a header with the job name
        headers.put("job-name", this.run.getNumber());
        // Add a header with the display name of the run
        headers.put("display-name", this.run.getDisplayName());
        // Add a header to stop the message from being displayed in the console
        headers.put("stop-message-console", remoteLoggingEnabled.get() ? "false" : "true");
        // Add a header with the display name of the run
        headers.put(HEADER_MACHINE_ID, MachineIdentifier.getUniqueMachineId());

        // Basic property
        AMQP.BasicProperties.Builder builder = RabbitMessageBuilder.build(new AMQP.BasicProperties.Builder(), this.property, headers, in -> Utils.injectEnvVars(run, envVars, in) );

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
                    lines.getBytes(StandardCharsets.UTF_8)
            );

            // Wait until publish is completed.
            try {
                PublishResult result = future.get();

                if (!result.isSuccess()) {
                    LOGGER.warning(LOG_HEADER + "Failed to publish message: " + lines);
                }
            } catch (Exception e) {
                LOGGER.warning(e.getMessage());
            }
        }
    }

    static class LineHandler {

        final List<String> logLines;
        boolean lineAdded = false;

        LineHandler(List<String> logLines) {
            this.logLines = logLines;
        }

        public void add(String line){
            if (!lineAdded) {
                lineAdded = logLines.add(line);
            }
        }

        public boolean hasLines(){
            return !logLines.isEmpty();
        }

        public String getLines(){
            String lines = String.join("\n", logLines);
            logLines.clear();
            return lines;
        }
    }

}
