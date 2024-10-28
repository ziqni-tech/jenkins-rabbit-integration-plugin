package com.ziqni.jenkins.plugins.rabbit.trigger;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.*;
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

import java.util.*;

/**
 * The extension trigger builds by application message.
 *
 * @author rinrinne a.k.a. rin_ne
 */
public class RabbitBuildTrigger<T extends Job<?, ?> & ParameterizedJobMixIn.ParameterizedJob<?,?>> extends Trigger<T> {

    public static final String PLUGIN_APPID = "robo-rabbit-remote-build";

    private static final String PLUGIN_NAME = Messages.rabbitMQBuildTrigger();

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
     * @param queueName
     *            the queue name.
     * @param jsonArray
     *            the content of application message.
     */
    public void scheduleBuild(String queueName, JSONArray jsonArray) {
        if (job != null) {
          if (jsonArray != null) {
              List<ParameterValue> parameters = getUpdatedParameters(jsonArray, getDefinitionParameters(job));
              ParameterizedJobMixIn.scheduleBuild2(job, 0, new CauseAction(new RabbitBuildCause(queueName)), new ParametersAction(parameters));
          } else {
              ParameterizedJobMixIn.scheduleBuild2(job, 0, new CauseAction(new RabbitBuildCause(queueName)));
          }
        }
    }

    /**
     * Gets updated parameters in job.
     *
     * @param jsonParameters
     *            the array of JSONObjects.
     * @param definedParameters
     *            the list of defined paramters.
     * @return the list of parameter values.
     */
    private List<ParameterValue> getUpdatedParameters(JSONArray jsonParameters, List<ParameterValue> definedParameters) {
        List<ParameterValue> newParams = new ArrayList<>();
        for (ParameterValue defParam : definedParameters) {

            for (int i = 0; i < jsonParameters.size(); i++) {
                JSONObject jsonParam = jsonParameters.getJSONObject(i);

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
}
