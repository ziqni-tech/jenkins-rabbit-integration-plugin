package com.ziqni.jenkins.plugins.rabbit.console;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import jenkins.model.ParameterizedJobMixIn;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.DataBoundConstructor;
import com.ziqni.jenkins.plugins.rabbit.trigger.RabbitBuildPublisher;

public class RabbitConsoleCollectorJobProperty extends JobProperty<Job<?, ?>> {

    private Boolean enableCollector;
    private String exchangeName;
    private String template;
    private String routingKey;
    private String startPublishingIfMessageContains;
    private String stopPublishingIfMessageContains;

    /**
     * Creates instance with specified parameters.
     *
     * @param enableCollector is enabled.
     * @param exchangeName the broker name.
     * @param routingKey the routing key.
     */
    @DataBoundConstructor
    public RabbitConsoleCollectorJobProperty(Boolean enableCollector, String exchangeName, String routingKey,
                                             String startPublishingIfMessageContains, String stopPublishingIfMessageContains,
                                             String template) {
        this.enableCollector = enableCollector;
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

    public boolean isEnableCollector() {
        return enableCollector;
    }

    public Boolean getEnableCollector() {
        return enableCollector;
    }

    @DataBoundSetter
    public void setEnableCollector(Boolean enableCollector) {
        this.enableCollector = enableCollector;
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

    public String getStartPublishingIfMessageContains() {
        return startPublishingIfMessageContains;
    }

    @DataBoundSetter
    public RabbitConsoleCollectorJobProperty setStartPublishingIfMessageContains(String startPublishingIfMessageContains) {
        this.startPublishingIfMessageContains = startPublishingIfMessageContains;
        return this;
    }

    public String getStopPublishingIfMessageContains() {
        return stopPublishingIfMessageContains;
    }

    @DataBoundSetter
    public RabbitConsoleCollectorJobProperty setStopPublishingIfMessageContains(String stopPublishingIfMessageContains) {
        this.stopPublishingIfMessageContains = stopPublishingIfMessageContains;
        return this;
    }

    public String getTemplate() {
        return template;
    }


    @DataBoundSetter
    public void setTemplate(String template) {
        this.template = template;
    }

    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {
        @Override
        public String getDisplayName() {
            return "Console Collector Configuration";
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return ParameterizedJobMixIn.ParameterizedJob.class.isAssignableFrom(jobType);
        }
    }
}
