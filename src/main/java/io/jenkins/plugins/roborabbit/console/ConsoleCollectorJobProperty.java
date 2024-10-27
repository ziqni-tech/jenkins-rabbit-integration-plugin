package io.jenkins.plugins.roborabbit.console;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import jenkins.model.ParameterizedJobMixIn;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.DataBoundConstructor;
import io.jenkins.plugins.roborabbit.trigger.RoboRabbitBuildPublisher;

public class ConsoleCollectorJobProperty extends JobProperty<Job<?, ?>> {

    private Boolean enableCollector;
    private String brokerName;
    private String routingKey;

    /**
     * Creates instance with specified parameters.
     *
     * @param enableCollector is enabled.
     * @param brokerName the broker name.
     * @param routingKey the routing key.
     */
    @DataBoundConstructor
    public ConsoleCollectorJobProperty(Boolean enableCollector, String brokerName, String routingKey) {
        this.enableCollector = enableCollector;
        this.brokerName = brokerName;
        if (StringUtils.isBlank(routingKey)) {
            this.routingKey = RoboRabbitBuildPublisher.class.getPackage().getName();
        } else {
            this.routingKey = routingKey;
        }
    }

    public Boolean getEnableCollector() {
        return enableCollector;
    }

    @DataBoundSetter
    public void setEnableCollector(Boolean enableCollector) {
        this.enableCollector = enableCollector;
    }

    public String getBrokerName() {
        return brokerName;
    }

    @DataBoundSetter
    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    @DataBoundSetter
    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
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
