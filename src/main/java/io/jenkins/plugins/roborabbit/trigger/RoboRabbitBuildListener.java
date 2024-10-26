package io.jenkins.plugins.roborabbit.trigger;

import hudson.Extension;
import io.jenkins.plugins.roborabbit.consumer.extensions.MessageQueueListener;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The extension listen application message then call triggers.
 *
 * @author rinrinne a.k.a. rin_ne
 */
@Extension
public class RoboRabbitBuildListener extends MessageQueueListener {
    private static final String PLUGIN_NAME = "Remote Builder";

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String KEY_PROJECT = "project";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_PARAMETER = "parameter";

    private static final Logger LOGGER = Logger.getLogger(RoboRabbitBuildListener.class.getName());

    private final Set<RoboRabbitBuildTrigger> triggers = new CopyOnWriteArraySet<RoboRabbitBuildTrigger>();

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public String getAppId() {
        return RoboRabbitBuildTrigger.PLUGIN_APPID;
    }

    /**
     * Get triggers.
     *
     * @return the set of {@link RoboRabbitBuildTrigger}.
     */
    public  Set<RoboRabbitBuildTrigger> getTriggers(){
        return this.triggers;
    }

    /**
     * Adds trigger.
     *
     * @param trigger
     *            the trigger.
     */
    public void addTrigger(RoboRabbitBuildTrigger trigger) {
        triggers.add(trigger);
    }

    /**
     * Removes trigger.
     *
     * @param trigger
     *            the trigger.
     */
    public void removeTrigger(RoboRabbitBuildTrigger trigger) {
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
        if (CONTENT_TYPE_JSON.equals(contentType)) {
            try {
                String msg = new String(body, "UTF-8");
                try {
                    JSONObject json = (JSONObject) JSONSerializer.toJSON(msg);
                    for (RoboRabbitBuildTrigger t : triggers) {

                        if (t.getRemoteBuildToken() == null) {
                            LOGGER.log(Level.WARNING, "ignoring AMQP trigger for project {0}: no token set", t.getProjectName());
                            continue;
                        }

                        if (t.getProjectName().equals(json.getString(KEY_PROJECT))
                                && t.getRemoteBuildToken().equals(json.getString(KEY_TOKEN))) {
                            if (json.containsKey(KEY_PARAMETER)) {
                                t.scheduleBuild(queueName, json.getJSONArray(KEY_PARAMETER));
                            } else {
                                t.scheduleBuild(queueName, null);
                            }
                        }
                    }
                } catch (JSONException e) {
                    LOGGER.warning("JSON format string: " + msg);
                    LOGGER.warning(e.getMessage());
                }
            } catch (UnsupportedEncodingException e) {
                LOGGER.warning("Unsupported encoding. Is message body is not string?");
            }
        }
    }
}
