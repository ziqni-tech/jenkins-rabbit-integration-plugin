package com.ziqni.jenkins.plugins.rabbit.console;

import com.rabbitmq.client.AMQP;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishChannel;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishChannelFactory;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishResult;
import com.ziqni.jenkins.plugins.rabbit.trigger.RabbitBuildTrigger;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.ziqni.jenkins.plugins.rabbit.trigger.RabbitBuildPublisher.*;

@Extension
public class RabbitConsoleOutputRunListener extends RunListener<Run> {

    private static final Logger LOGGER = Logger.getLogger(RabbitConsoleOutputRunListener.class.getName());
    private RabbitConsoleCollectorJobProperty property;

    @Override
    public void onStarted(Run run, TaskListener listener) {
        this.property = getJobProperty(run);
        super.onStarted(run, listener);
        try {
            // Attach your custom console log filter to the build
            run.getExecutor().getOwner().getChannel().setConsoleLogFilter(new RabbitCustomConsoleLogFilter());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCompleted(Run run, TaskListener listener) {
        listener.getLogger().println("[RabbitConsoleOutputRunListener] Build completed: " + run.getFullDisplayName());

        // Access console output
        try {
            String consoleOutput = run.getLog();

            // Generate message (JSON format)
            JSONObject json = new JSONObject();
//        json.put(KEY_PROJECT, build.getProject().getName());
//        json.put(KEY_NUMBER, build.getNumber());
//        json.put(KEY_STATUS, getResultAsString(build.getResult()));

            // Basic property
            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
            builder.appId(RabbitBuildTrigger.PLUGIN_APPID);
            builder.contentType(JSON_CONTENT_TYPE);

            // Header
            Map<String, Object> headers = new HashMap<String, Object>();
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins != null) {
                headers.put(HEADER_JENKINS_URL, jenkins.getRootUrl());
            }
            builder.headers(headers);

            // Publish message
            PublishChannel ch = PublishChannelFactory.getPublishChannel();
            if (ch != null && ch.isOpen()) {
                // return value is not needed if you don't need to wait.
                Future<PublishResult> future = ch.publish(this.property.getExchangeName(), this.property.getRoutingKey(), builder.build(),
                        json.toString().getBytes(StandardCharsets.UTF_8));

                // Wait until publish is completed.
                try {
                    PublishResult result = future.get();

                    if (result.isSuccess()) {
                        listener.getLogger().println(LOG_HEADER + "Success.");
                    } else {
                        listener.getLogger().println(LOG_HEADER + "Fail - " + result.getMessage());
                    }
                } catch (Exception e) {
                    LOGGER.warning(e.getMessage());
                    listener.getLogger().println(LOG_HEADER + "Fail due to exception.");
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading console output for build " + run.getFullDisplayName(), e);
        }
    }

    @Override
    public void onFinalized(Run run) {
        LOGGER.log(Level.INFO, "[RabbitConsoleOutputRunListener] Build finalized: {0}", run.getFullDisplayName());
    }

    private RabbitConsoleCollectorJobProperty getJobProperty(Run run) {
        if (Objects.nonNull(run)) {
            Job<?, ?> job = (Job<?, ?>) run.getParent();
            return job.getProperty(RabbitConsoleCollectorJobProperty.class);
        }
        return null;
    }
}
