package com.ziqni.jenkins.plugins.rabbit.trigger;

import com.rabbitmq.client.AMQP.BasicProperties;

import com.ziqni.jenkins.plugins.rabbit.utils.*;
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
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.concurrent.Future;
import java.nio.charset.StandardCharsets;

import static com.ziqni.jenkins.plugins.rabbit.utils.MachineIdentifier.HEADER_MACHINE_ID;

/**
 * The extension publish build result using rabbitmq.
 *
 * @author rinrinne a.k.a. rin_ne
 */
public class RabbitBuildPublisher extends Notifier implements RabbitMessageBuilderAccessors {

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
    private String contentType;
    private String contentEncoding;
    private String headers;
    private Integer deliveryMode;
    private Integer priority;
    private String correlationId;
    private String replyTo;
    private String expiration;
    private String messageId;
    private String timestamp;
    private String type;
    private String userId;
    private String appId;
    private String clusterId;

    /**
     * Creates instance with specified parameters.
     *
     * @param exchangeName      the exchange name.
     * @param routingKey        the routing key.
     * @param template          the custom template (optional).
     * @param contentType       the content type.
     * @param contentEncoding   the content encoding.
     * @param headers           headers in JSON or key=value format.
     * @param deliveryMode      the delivery mode.
     * @param priority          the priority.
     * @param correlationId     the correlation ID.
     * @param replyTo           the reply-to address.
     * @param expiration        the expiration time.
     * @param messageId         the message ID.
     * @param timestamp         the timestamp.
     * @param type              the message type.
     * @param userId            the user ID.
     * @param appId             the application ID.
     * @param clusterId         the cluster ID.
     */
    @DataBoundConstructor
    public RabbitBuildPublisher(
            String exchangeName,
            String routingKey,
            String template,
            String contentType,
            String contentEncoding,
            String headers,
            Integer deliveryMode,
            Integer priority,
            String correlationId,
            String replyTo,
            String expiration,
            String messageId,
            String timestamp,
            String type,
            String userId,
            String appId,
            String clusterId) {

        this.exchangeName = exchangeName;
        this.routingKey = StringUtils.isBlank(routingKey) ? RabbitBuildPublisher.class.getPackage().getName() : routingKey;
        this.template = StringUtils.isBlank(template) ? null : template;

        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
        this.headers = headers;
        this.deliveryMode = deliveryMode;
        this.priority = priority;
        this.correlationId = correlationId;
        this.replyTo = replyTo;
        this.expiration = expiration;
        this.messageId = messageId;
        this.timestamp = timestamp;
        this.type = type;
        this.userId = userId;
        this.appId = appId;
        this.clusterId = clusterId;
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

        // Get environment variables from the build
        Map<String, String> envVars = build.getEnvironment(listener);
        envVars.put("BUILD_STATUS",getResultAsString(build.getResult()));

        if (exchangeName == null || exchangeName.isEmpty()) {
            return true;
        }

        // Header
        Map<String, Object> headers = new HashMap<>();
        Jenkins jenkins = Jenkins.get();
        headers.put(HEADER_JENKINS_URL, jenkins.getRootUrl());
        headers.put(HEADER_MACHINE_ID, MachineIdentifier.getUniqueMachineId());

        // Basic property
        BasicProperties.Builder builder = RabbitMessageBuilder.build(new BasicProperties.Builder(), this, headers, in -> Utils.injectEnvVars(build,envVars, in) );

        builder.appId(RabbitBuildTrigger.PLUGIN_APPID);

        if(StringUtils.isBlank(this.template)) {
            builder.contentType(JSON_CONTENT_TYPE);
        }

        // Publish message
        PublishChannel ch = PublishChannelFactory.getPublishChannel();
        if (ch != null && ch.isOpen()) {
            // return value is not needed if you don't need to wait.
            String routingKeyReady = Utils.injectEnvVars(build, envVars, routingKey);
            String response = prepareResponse(build, envVars);

            Future<PublishResult> future = ch.publish(
                    exchangeName,
                    routingKeyReady,
                    builder.build(),
                    response.getBytes(StandardCharsets.UTF_8)
            );

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
        return Utils.injectEnvVars(build, envVars, this.template, () -> {
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
            return Messages.rabbitBuildPublisher();
        }
    }

    public String getExchangeName() {
        return exchangeName;
    }

    @DataBoundSetter
    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    @DataBoundSetter
    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getTemplate() {
        return template;
    }

    @DataBoundSetter
    public void setTemplate(String template) {
        this.template = template;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    @DataBoundSetter
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getContentEncoding() {
        return contentEncoding;
    }

    @Override
    @DataBoundSetter
    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    @Override
    public String getHeaders() {
        return headers;
    }

    @DataBoundSetter
    public void setHeaders(String headers) {
        this.headers = headers;
    }

    @Override
    public Integer getDeliveryMode() {
        return deliveryMode;
    }

    @Override
    @DataBoundSetter
    public void setDeliveryMode(Integer deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    @Override
    public Integer getPriority() {
        return priority;
    }

    @Override
    @DataBoundSetter
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    @DataBoundSetter
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public String getReplyTo() {
        return replyTo;
    }

    @Override
    @DataBoundSetter
    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    @Override
    public String getExpiration() {
        return expiration;
    }

    @Override
    @DataBoundSetter
    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    @Override
    @DataBoundSetter
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public String getTimestamp() {
        return timestamp;
    }

    @Override
    @DataBoundSetter
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    @DataBoundSetter
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    @DataBoundSetter
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String getAppId() {
        return appId;
    }

    @Override
    @DataBoundSetter
    public void setAppId(String appId) {
        this.appId = appId;
    }

    @Override
    public String getClusterId() {
        return clusterId;
    }

    @Override
    @DataBoundSetter
    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }
}
