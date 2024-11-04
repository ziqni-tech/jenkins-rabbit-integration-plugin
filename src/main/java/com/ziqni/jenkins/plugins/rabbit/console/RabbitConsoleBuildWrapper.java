package com.ziqni.jenkins.plugins.rabbit.console;

import com.ziqni.jenkins.plugins.rabbit.utils.RabbitMessageBuilderAccessors;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.console.ConsoleLogFilter;
import hudson.model.*;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.tasks.SimpleBuildWrapper;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.DataBoundConstructor;
import com.ziqni.jenkins.plugins.rabbit.trigger.RabbitBuildPublisher;

import java.io.IOException;
import java.util.logging.Logger;

public class RabbitConsoleBuildWrapper extends SimpleBuildWrapper implements RabbitMessageBuilderAccessors {

    private static final Logger LOGGER = Logger.getLogger(RabbitConsoleBuildWrapper.class.getName());

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
    private String startPublishingIfMessageContains;
    private String stopPublishingIfMessageContains;
    private Boolean enableBundling=false;
    private Boolean excludeStartLine=false;
    private Boolean excludeStopLine =false;

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
     * @param exchangeName the broker name.
     * @param routingKey the routing key.
     * @param enableBundling enable bundling.
     * @param excludeStartLine exclude start line.
     * @param excludeStopLine exclude stop line.
     */
    @DataBoundConstructor
    public RabbitConsoleBuildWrapper(
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
            String clusterId,
            String startPublishingIfMessageContains,
            String stopPublishingIfMessageContains,
            Boolean enableBundling,
            Boolean excludeStartLine,
            Boolean excludeStopLine) {

        this.exchangeName = exchangeName;
        this.template = template;

        if (StringUtils.isBlank(routingKey)) {
            this.routingKey = RabbitBuildPublisher.class.getPackage().getName();
        } else {
            this.routingKey = routingKey;
        }

        if (StringUtils.isBlank(startPublishingIfMessageContains)) {
            this.startPublishingIfMessageContains = null;
        } else {
            this.startPublishingIfMessageContains = startPublishingIfMessageContains;
        }

        if (StringUtils.isBlank(stopPublishingIfMessageContains)) {
            this.stopPublishingIfMessageContains = null;
        } else {
            this.stopPublishingIfMessageContains = stopPublishingIfMessageContains;
        }

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
        this.enableBundling = enableBundling;
        this.excludeStartLine = excludeStartLine;
        this.excludeStopLine = excludeStopLine;

    }

    public String getExchangeName() {
        return exchangeName;
    }

    public void notifyJobCompletion(Run<?, ?> run, TaskListener listener) {
        // Add your notification logic here (e.g., sending a message to RabbitMQ)
        listener.getLogger().println("RabbitConsoleBuildWrapper: Job has completed.");
        LOGGER.info("RabbitConsoleBuildWrapper: Job completed for " + run.getParent().getFullName());

        // Example: Send a message to RabbitMQ (placeholder logic)
        // RabbitMQPublisher.publish(exchangeName, routingKey, "Job completed");
    }

    @Override
    public void setUp(Context context, Run<?, ?> build, TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {
        listener.getLogger().println("RabbitConsoleLogBuildWrapper: Setting up environment...");
        // You can add environment variables here if needed
        context.env("RABBIT_ROUTING_KEY", routingKey);
    }

    @Override
    public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {
        listener.getLogger().println("RabbitConsoleLogBuildWrapper: Setting up environment...");
        // You can add environment variables here if needed
        context.env("RABBIT_ROUTING_KEY", routingKey);
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

    public String getStartPublishingIfMessageContains() {
        return startPublishingIfMessageContains;
    }

    @DataBoundSetter
    public void setStartPublishingIfMessageContains(String startPublishingIfMessageContains) {
        this.startPublishingIfMessageContains = startPublishingIfMessageContains;
    }

    public String getStopPublishingIfMessageContains() {
        return stopPublishingIfMessageContains;
    }

    @DataBoundSetter
    public void setStopPublishingIfMessageContains(String stopPublishingIfMessageContains) {
        this.stopPublishingIfMessageContains = stopPublishingIfMessageContains;
    }

    public String getTemplate() {
        return template;
    }


    @DataBoundSetter
    public void setTemplate(String template) {
        this.template = template;
    }

    @Override
    public ConsoleLogFilter createLoggerDecorator(Run<?, ?> run) {
        // Ensure that this returns a filter to decorate the console log
        return new RabbitConsoleLogFilter(TaskListener.NULL, this);
    }

    public String getContentType() {
        return contentType;
    }

    @DataBoundSetter
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    @DataBoundSetter
    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public String getHeaders() {
        return headers;
    }

    @DataBoundSetter
    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public Integer getDeliveryMode() {
        return deliveryMode;
    }

    @DataBoundSetter
    public void setDeliveryMode(Integer deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public Integer getPriority() {
        return priority;
    }

    @DataBoundSetter
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    @DataBoundSetter
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getReplyTo() {
        return replyTo;
    }

    @DataBoundSetter
    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getExpiration() {
        return expiration;
    }

    @DataBoundSetter
    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }

    public String getMessageId() {
        return messageId;
    }

    @DataBoundSetter
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @DataBoundSetter
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    @DataBoundSetter
    public void setType(String type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    @DataBoundSetter
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAppId() {
        return appId;
    }

    @DataBoundSetter
    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getClusterId() {
        return clusterId;
    }

    @DataBoundSetter
    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public Boolean getEnableBundling() {
        return enableBundling;
    }

    @DataBoundSetter
    public void setEnableBundling(Boolean enableBundling) {
        this.enableBundling = enableBundling;
    }

    public Boolean getExcludeStartLine() {
        return excludeStartLine;
    }

    @DataBoundSetter
    public void setExcludeStartLine(Boolean excludeStartLine) {
        this.excludeStartLine = excludeStartLine;
    }

    public Boolean getExcludeStopLine() {
        return excludeStopLine;
    }

    @DataBoundSetter
    public void setExcludeStopLine(Boolean excludeStopLine) {
        this.excludeStopLine = excludeStopLine;
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public String getDisplayName() {
            return "RabbitMQ console collector";
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }
    }
}
