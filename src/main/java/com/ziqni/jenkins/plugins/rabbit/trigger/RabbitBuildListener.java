package com.ziqni.jenkins.plugins.rabbit.trigger;

import com.ziqni.jenkins.plugins.rabbit.utils.MachineIdentifier;
import hudson.Extension;
import net.sf.json.JSONObject;
import net.sf.json.JSONException;
import net.sf.json.JSONSerializer;
import com.ziqni.jenkins.plugins.rabbit.consumer.extensions.MessageQueueListener;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.ziqni.jenkins.plugins.rabbit.utils.MachineIdentifier.HEADER_MACHINE_ID;

/**
 * The extension listen application message then call triggers.
 *
 * @author rinrinne a.k.a. rin_ne
 */
@Extension
public class RabbitBuildListener extends MessageQueueListener {
    private static final String PLUGIN_NAME = "Remote Builder";

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String KEY_PROJECT = "project";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_PARAMETER = "parameter";

    private static final Logger LOGGER = Logger.getLogger(RabbitBuildListener.class.getName());

    private final Set<RabbitBuildTrigger<?>> triggers = new CopyOnWriteArraySet<>();

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public String getAppId() {
        return RabbitBuildTrigger.PLUGIN_APPID;
    }

    /**
     * Get triggers.
     *
     * @return the set of {@link RabbitBuildTrigger}.
     */
    public  Set<RabbitBuildTrigger<?>> getTriggers(){
        return this.triggers;
    }

    /**
     * Adds trigger.
     *
     * @param trigger
     *            the trigger.
     */
    public void addTrigger(RabbitBuildTrigger<?> trigger) {
        triggers.add(trigger);
    }

    /**
     * Removes trigger.
     *
     * @param trigger
     *            the trigger.
     */
    public void removeTrigger(RabbitBuildTrigger<?> trigger) {
        triggers.remove(trigger);
    }

    @Override
    public void onBind(String queueName) {
        LOGGER.info("Bind to: " + queueName);
    }

    @Override
    public void onUnbind(String queueName) {
        LOGGER.info("Unbind from: " + queueName);
    }

    /**
     * Finds matched projects using given project name and token then schedule
     * build.
     */
    @Override
    public void onReceive(String queueName, String contentType, Map<String, Object> headers, byte[] body) {

        String msg = new String(body, StandardCharsets.UTF_8).trim();

        if(headers.containsKey(HEADER_MACHINE_ID) && headers.get(HEADER_MACHINE_ID).toString().equals(MachineIdentifier.getUniqueMachineId())){
            LOGGER.log(Level.WARNING, "This is a recursive call from within the system. Blocking for all projects, caused by {0}", headers.get(HEADER_MACHINE_ID));
            return;
        }

        try {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(msg);
            for (RabbitBuildTrigger<?> rabbitBuildTrigger : triggers) {

                final var tokenReceived = json.containsKey(KEY_TOKEN) ? json.getString(KEY_TOKEN) : null;
                final var tokenOk = tokenMatched(tokenReceived,rabbitBuildTrigger.getRemoteBuildToken());

                if (!tokenOk) {
                    LOGGER.log(Level.WARNING, "Token mismatched for project {0}", rabbitBuildTrigger.getProjectName());
                    continue;
                }

                if (rabbitBuildTrigger.getProjectName().equals(json.getString(KEY_PROJECT))) {
                    if (json.containsKey(KEY_PARAMETER)) {
                        rabbitBuildTrigger.scheduleBuild(queueName, json.getJSONArray(KEY_PARAMETER));
                    } else {
                        rabbitBuildTrigger.scheduleBuild(queueName, null);
                    }
                }
            }
        } catch (JSONException e) {
            LOGGER.warning("JSON format string: " + msg);
            LOGGER.warning(e.getMessage());
        }
    }

    private boolean tokenMatched(String tokenReceived, String tokenConfigured) {
        if(Objects.isNull(tokenConfigured) || tokenConfigured.isEmpty())
            return true;

        return tokenConfigured.equals(tokenReceived);
    }
}
