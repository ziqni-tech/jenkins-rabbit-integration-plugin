package com.ziqni.jenkins.plugins.rabbit.trigger;

import com.rabbitmq.client.AMQP.BasicProperties;

import com.ziqni.jenkins.plugins.rabbit.utils.Utils;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.Result;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.BuildStepDescriptor;

import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishResult;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishChannel;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishChannelFactory;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.concurrent.Future;
import java.nio.charset.StandardCharsets;

/**
 * The extension publish build result using rabbitmq.
 *
 * @author rinrinne a.k.a. rin_ne
 */
public class RabbitBuildPublisher extends Notifier {

    private static final Logger LOGGER = Logger.getLogger(RabbitBuildPublisher.class.getName());

    private static final String KEY_PROJECT = "project";
    private static final String KEY_NUMBER = "number";
    private static final String KEY_STATUS = "status";

    public static final String HEADER_JENKINS_URL = "jenkins-url";
    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String TEXT_CONTENT_TYPE = "text/plain";

    public static final String LOG_HEADER = "Publish to RabbitMQ: ";

    private String exchangeName;
    private String routingKey;
    private String template;

    /**
     * Creates instance with specified parameters.
     *
     * @param exchangeName the exchange name.
     * @param routingKey the routing key.
     */
    @DataBoundConstructor
    public RabbitBuildPublisher(String exchangeName, String routingKey, String template) {
        this.exchangeName = exchangeName;
        if (StringUtils.isBlank(routingKey)) {
            this.routingKey = RabbitBuildPublisher.class.getPackage().getName();
        } else {
            this.routingKey = routingKey;
        }
        if (template == null || StringUtils.isBlank(template.trim())) {
            this.template = null;
        } else {
            this.template = template;
        }
    }

    /**
     * Gets exchange name.
     *
     * @return the exchange name.
     */
    public String getExchangeName() {
        return exchangeName;
    }

    /**
     * Sets exchange name.
     *
     * @param exchangeName the exchange name.
     */
    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    /**
     * Gets routingKey.
     *
     * @return the routingKey.
     */
    public String getRoutingKey() {
        return routingKey;
    }

    /**
     * Sets routingKey.
     *
     * @param routingKey the routingKey.
     */
    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    /**
     * Gets result as string.
     *
     * @param result the result.
     * @return the result string.
     */
    private String getResultAsString(Result result) {
        String retStr = "ONGOING";
        if (result != null) {
            retStr = result.toString();
       }
       return retStr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        if (exchangeName == null || exchangeName.isEmpty()) {
            return true;
        }

        // Get environment variables from the build
        Map<String, String> envVars = build.getEnvironment(listener);

        // Basic property
        BasicProperties.Builder builder = new BasicProperties.Builder();
        builder.appId(RabbitBuildTrigger.PLUGIN_APPID);

        if(Objects.nonNull(this.template)) {
            builder.contentType(JSON_CONTENT_TYPE);
        }

        // Header
        Map<String, Object> headers = new HashMap<>();
        Jenkins jenkins = Jenkins.get();
        headers.put(HEADER_JENKINS_URL, jenkins.getRootUrl());
        builder.headers(headers);

        // Publish message
        PublishChannel ch = PublishChannelFactory.getPublishChannel();
        if (ch != null && ch.isOpen()) {
            // return value is not needed if you don't need to wait.
            String jsonString = prepareResponse(build, envVars);

            Future<PublishResult> future = ch.publish(exchangeName, routingKey, builder.build(), jsonString.getBytes(StandardCharsets.UTF_8));

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
        return true;
    }

    /**
     * Prepare response.
     * @param build build
     * @param envVars environment variables
     * @return response
     */
    private String prepareResponse(AbstractBuild<?, ?> build, Map<String, String> envVars){
        return Utils.prepareResponse(build, envVars, this.template, () -> {
            // Generate message (JSON format)
            JSONObject json = new JSONObject();
            json.put(KEY_PROJECT, build.getProject().getName());
            json.put(KEY_NUMBER, build.getNumber());
            json.put(KEY_STATUS, getResultAsString(build.getResult()));
            return json.toString();
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * The descriptor for this publisher.
     *
     * @author rinrinne a.k.a. rin_ne
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> project) {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return Messages.roboRabbitBuildPublisher();
        }
    }
}
