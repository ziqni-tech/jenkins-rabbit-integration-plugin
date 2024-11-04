package com.ziqni.jenkins.plugins.rabbit.trigger;

import com.rabbitmq.client.AMQP;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishChannel;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishChannelFactory;
import com.ziqni.jenkins.plugins.rabbit.consumer.publishers.PublishResult;
import com.ziqni.jenkins.plugins.rabbit.utils.MachineIdentifier;
import com.ziqni.jenkins.plugins.rabbit.utils.RabbitMessageBuilder;
import com.ziqni.jenkins.plugins.rabbit.utils.RabbitMessageProperties;
import com.ziqni.jenkins.plugins.rabbit.utils.Utils;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.Queue;
import hudson.model.listeners.ItemListener;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import com.ziqni.jenkins.plugins.rabbit.consumer.extensions.MessageQueueListener;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import static com.ziqni.jenkins.plugins.rabbit.trigger.JobRequestConstraints.CONFIRM_RECEIPT;
import static com.ziqni.jenkins.plugins.rabbit.utils.MachineIdentifier.HEADER_MACHINE_ID;

/**
 * The extension trigger builds by application message.
 *
 * @author rinrinne a.k.a. rin_ne
 */
public class RabbitBuildTrigger<T extends Job<?, ?> & ParameterizedJobMixIn.ParameterizedJob<?,?>> extends Trigger<T> {

    private static final Logger LOGGER = Logger.getLogger(RabbitBuildTrigger.class.getName());

    public static final String LOG_HEADER = "Publish to RabbitMQ from Rabbit Build Trigger: ";
    public static final String HEADER_JENKINS_URL = "jenkins-url";
    public static final String JSON_CONTENT_TYPE = "application/json";

    public static final String PLUGIN_APPID = "robo-rabbit-remote-build";

    private static final String PLUGIN_NAME = Messages.rabbitBuildTrigger();

    private static final String KEY_PARAM_NAME = "name";
    private static final String KEY_PARAM_VALUE = "value";

    private String remoteBuildToken;

    /**
     * Creates instance with specified parameters.
     *
     * @param remoteBuildToken
     *            the token for remote build.
     */
    @DataBoundConstructor
    public RabbitBuildTrigger(String remoteBuildToken) {
        super();
        this.remoteBuildToken = StringUtils.stripToNull(remoteBuildToken);
    }

    @Override
    public void start(T project, boolean newInstance) {
        RabbitBuildListener listener = MessageQueueListener.all().get(RabbitBuildListener.class);

        if (listener != null) {
            listener.addTrigger(this);
            removeDuplicatedTrigger(listener.getTriggers());
        }
        super.start(project, newInstance);
    }

    @Override
    public void stop() {
        RabbitBuildListener listener = MessageQueueListener.all().get(RabbitBuildListener.class);
        if (listener != null) {
            listener.removeTrigger(this);
        }
        super.stop();
    }

    /**
     * Remove the duplicated trigger from the triggers.
     *
     * @param triggers
     *          the set of current trigger instances which have already been loaded in the memory
     */
    public void removeDuplicatedTrigger(Set<RabbitBuildTrigger<?>> triggers){
        Map<String, RabbitBuildTrigger<?>>  tempHashMap= new HashMap<>();
        for(RabbitBuildTrigger<?> trigger:triggers){
            tempHashMap.put(trigger.getProjectName(), trigger);
        }
        triggers.clear();
        triggers.addAll(tempHashMap.values());
    }

    /**
     * Gets token.
     *
     * @return the token.
     */
    public String getRemoteBuildToken() {
        return remoteBuildToken;
    }

    /**
     * Sets token.
     *
     * @param remoteBuildToken the token.
     */
    public void setRemoteBuildToken(String remoteBuildToken) {
        this.remoteBuildToken = remoteBuildToken;
    }

    /**
     * Gets project name.
     *
     * @return the project name.
     */
    public String getProjectName() {
        if(job!=null){
            return job.getFullName();
        }
        return "";
    }

    /**
     * Schedules build for triggered job using application message.
     *
     * @param props         the properties of application message.
     * @param parametersArray     the content of application message.
     * @param constraintSet the set of constraints.
     */
    public void scheduleBuild(RabbitMessageProperties props, JSONArray parametersArray, Set<String> constraintSet) {
        if (job != null) {

            final var cause = new RabbitBuildCause(props);

            if (parametersArray != null) {
                List<ParameterValue> parameters = getUpdatedParameters(parametersArray, getDefinitionParameters(job));
                handleQueueItem(props,constraintSet, ParameterizedJobMixIn.scheduleBuild2(job, 0, new CauseAction(cause), new ParametersAction(parameters), new EnvironmentVariablesAction(props)) );
            } else {
                handleQueueItem(props,constraintSet, ParameterizedJobMixIn.scheduleBuild2(job, 0, new CauseAction(cause), new EnvironmentVariablesAction(props)) );
            }
        }
    }

    private void handleQueueItem(RabbitMessageProperties props, Set<String> constraintSet, Queue.Item item) {
        if (item.task instanceof Job) {
            Job<?, ?> job = (Job<?, ?>) item.task;

            if(constraintSet.contains(CONFIRM_RECEIPT.getValue())) {
                try {
                    confirmReceipt(props, job);
                } catch (Exception e) {
                    LOGGER.warning(e.getMessage());
                }
            }
        }

    }

    /**
     * Gets updated parameters in job.
     *
     * @param parametersArray
     *            the array of JSONObjects.
     * @param definedParameters
     *            the list of defined paramters.
     * @return the list of parameter values.
     */
    private List<ParameterValue> getUpdatedParameters(JSONArray parametersArray, List<ParameterValue> definedParameters) {
        List<ParameterValue> newParams = new ArrayList<>();
        for (ParameterValue defParam : definedParameters) {

            for (int i = 0; i < parametersArray.size(); i++) {
                JSONObject jsonParam = parametersArray.getJSONObject(i);

                if (defParam.getName().equalsIgnoreCase(jsonParam.getString(KEY_PARAM_NAME))) {
                    newParams.add(new StringParameterValue(defParam.getName(), jsonParam.getString(KEY_PARAM_VALUE)));
                }
            }
        }
        return newParams;
    }

    /**
     * Gets definition parameters.
     *
     * @param project
     *            the project.
     * @return the list of parameter values.
     */
    private List<ParameterValue> getDefinitionParameters(Job<?, ?> project) {
        List<ParameterValue> parameters = new ArrayList<>();
        ParametersDefinitionProperty properties = project
                .getProperty(ParametersDefinitionProperty.class);

        if (properties != null) {
            for (ParameterDefinition paramDef : properties.getParameterDefinitions()) {
                ParameterValue param = paramDef.getDefaultParameterValue();
                if (param != null) {
                    parameters.add(param);
                }
            }
        }

        return parameters;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * The descriptor for this trigger.
     *
     * @author rinrinne a.k.a. rin_ne
     */
    @Extension @Symbol("rmqRemoteBuild")
    public static class DescriptorImpl extends TriggerDescriptor {

        @Override
        public boolean isApplicable(Item item) {
            return true;
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return PLUGIN_NAME;
        }

        /**
         * ItemListener implementation class.
         *
         * @author rinrinne a.k.a. rin_ne
         */
        @Extension
        public static class ItemListenerImpl extends ItemListener {

            @Override
            public void onLoaded() {
                RabbitBuildListener listener = MessageQueueListener.all().get(RabbitBuildListener.class);
                Jenkins jenkins = Jenkins.get();
                for (Project<?, ?> p : jenkins.getAllItems(Project.class)) {
                    RabbitBuildTrigger<?> t = p.getTrigger(RabbitBuildTrigger.class);
                    if (t != null && listener != null) {
                        listener.addTrigger(t);
                    }
                }
            }
        }
    }

    public boolean confirmReceipt(RabbitMessageProperties props, Job<?, ?> job)
            throws InterruptedException, IOException {

        // Header
        Map<String, Object> headers = new HashMap<>();
        Jenkins jenkins = Jenkins.get();
        headers.put(HEADER_JENKINS_URL, jenkins.getRootUrl());
        headers.put(HEADER_MACHINE_ID, MachineIdentifier.getUniqueMachineId());

        // Basic property
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        builder.replyTo("no-reply");
        builder.contentType(JSON_CONTENT_TYPE);
        builder.appId(RabbitBuildTrigger.PLUGIN_APPID);
        builder.correlationId(props.getCorrelationId());

        // Publish message
        PublishChannel ch = PublishChannelFactory.getPublishChannel();
        if (ch != null && ch.isOpen()) {
            // return value is not needed if you don't need to wait.
            String response = JobInfoMapper.createJobInfoJson(job).toString();

            Future<PublishResult> future = ch.publish(
                    props.getExchange(),
                    props.getReplyTo(),
                    builder.build(),
                    response.getBytes(StandardCharsets.UTF_8)
            );

            // Wait until publish is completed.
            try {
                PublishResult result = future.get();
            } catch (Exception e) {
                LOGGER.warning(e.getMessage());
            }
        }
        return true;
    }
}
