package com.ziqni.jenkins.plugins.rabbit.console;

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

public class RabbitConsoleCollectorJobProperty extends SimpleBuildWrapper {

    private String exchangeName;
    private String template;
    private String routingKey;
    private String startPublishingIfMessageContains;
    private String stopPublishingIfMessageContains;

    /**
     * Creates instance with specified parameters.
     *
     * @param exchangeName the broker name.
     * @param routingKey the routing key.
     */
    @DataBoundConstructor
    public RabbitConsoleCollectorJobProperty(String exchangeName, String routingKey,
                                             String startPublishingIfMessageContains, String stopPublishingIfMessageContains,
                                             String template) {
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
    }

    public String getExchangeName() {
        return exchangeName;
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
